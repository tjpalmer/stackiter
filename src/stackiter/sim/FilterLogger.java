package stackiter.sim;

import static stackiter.sim.Util.*;

import java.awt.geom.*;
import java.util.*;

/**
 * Logs just important key states to an underlying logger.
 */
public class FilterLogger extends AtomicLogger {

	private static class Grasp {
		Block item;
		Point2D pointRelItem;
		Tool tool;
	}

	private static class Release {
		Block item;
		Tool tool;
	}

	/**
	 * What counts as "zero" for important time steps.
	 */
	public static double ANGULAR_ACCELERATION_EPSILON = 5;

	/**
	 * An overall throttle on noisy events. We ignore events even across items,
	 * if too closely spaced in time.
	 */
	public static double BUSY_NOISE_DURATION = 0.05;

	/**
	 * What counts as "zero" for important time steps.
	 */
	public static double LINEAR_ACCELERATION_EPSILON = 50;

	/**
	 * How long a noisy event should be off before we notice it again.
	 */
	public static double NOISY_EVENT_DURATION = 0.5;

	private boolean cleared;

	private Point2D displaySize = point();

	private Set<Object> everLogged = new HashSet<Object>();

	/**
	 * LinkedHashMap preserves add order.
	 */
	private Map<Object, Grasp> grasps = new LinkedHashMap<Object, Grasp>();

	/**
	 * LinkedHashMap preserves add order.
	 */
	private Map<Object, Item> items = new LinkedHashMap<Object, Item>();

	private double simTime;

	private long steps;

	/**
	 * LinkedHashMap preserves add order.
	 */
	private Map<Object, Tool> tools = new LinkedHashMap<Object, Tool>();

	private boolean logWanted;

	private final Logger logger;

	private Map<Soul, Double> noisyEventTimes = new HashMap<Soul, Double>();

	private Map<Soul, Release> releases = new LinkedHashMap<Soul, Release>();

	private List<Item> removals = new ArrayList<Item>();

	/**
	 * LinkedHashMap preserves add order.
	 */
	private Map<Object, Boolean> toolPresents = new LinkedHashMap<Object, Boolean>();

	private Tray tray;

	private Rectangle2D view = rectangle();

	public FilterLogger(Logger logger) {
		this.logger = logger;
	}

	private void clearFinishedNoisyEvents() {
		if (!noisyEventTimes.isEmpty()) {
			for (Iterator<Map.Entry<Soul, Double>> e = noisyEventTimes.entrySet().iterator(); e.hasNext();) {
				Map.Entry<Soul, Double> entry = e.next();
				if (simTime - entry.getValue() > NOISY_EVENT_DURATION) {
					// The event has expired.
					e.remove();
				}
			}
			if (noisyEventTimes.isEmpty()) {
				// Everything has stopped moving. Let's see what's up.
				// TODO Log whenever _anything_ (rather than everything) stops?
				requestLog();
				//System.out.println("$");
			}
		}
	}

	@Override
	public void close() {
		requestLog();
		logger.close();
	}

	@Override
	protected void endStep() {
		super.endStep();
		logStateIfReadyAndWanted();
	}

	@Override
	public void flush() {
		logger.flush();
	}

	@Override
	public void logClear() {
		cleared = true;
		// Log clears.
		requestLog();
	}

	@Override
	public void logDisplaySize(Point2D size) {
		displaySize.setLocation(size);
	}

	@Override
	public void logGrasp(Tool tool, Block item, Point2D pointRelItem) {

		if (releases.containsKey(tool.getSoul())) {
			// TODO Actually, we probably want to know which grasp was most recently _logged_ before making decisions like this.
			releases.remove(tool.getSoul());
		}

		Grasp grasp = new Grasp();
		// TODO I doubt we need to clone tool or item since just their ids are needed in the end.
		grasp.tool = tool;
		grasp.item = item;
		grasp.pointRelItem = copy(pointRelItem);
		grasps.put(tool.getSoul(), grasp);

		// Log grasps.
		requestLog();

	}

	@Override
	public void logItem(Item item) {
		Item old = items.get(item.getSoul());
		if (old != null) {
			// See if the acceleration changes indicate an event we want to track.
			if (
				crossedZero(item.getAngularAcceleration(), old.getAngularAcceleration(), ANGULAR_ACCELERATION_EPSILON) ||
				crossedZero(item.getLinearAcceleration(), old.getLinearAcceleration(), LINEAR_ACCELERATION_EPSILON)
			) {
				trackNoisyEvent(item.getSoul());
			}
		}
		// Now update the item, in any case.
		items.put(item.getSoul(), item.clone());
	}

	@Override
	public void logRelease(Tool tool, Block item) {
		if (grasps.containsKey(tool.getSoul())) {
			// Yet unlogged grasp can just go away.
			grasps.remove(tool.getSoul());
		} else {
			logTool(tool);
			logItem(item);
			// No clone. We just want the ID.
			Release release = new Release();
			release.item = item;
			release.tool = tool;
			releases.put(tool.getSoul(), release);
		}
		// We want to log releases.
		requestLog();
	}

	@Override
	public void logRemoval(Item item) {
		if (everLogged.contains(item.getSoul())) {
			removals.add(item);
		} else {
			// Never seen. Just ignore it.
			// TODO Do any outstanding releases reference it?
			items.remove(item.getSoul());
			// TODO Remove associated noisy events?
		}
	}

	@Override
	public void logSimTime(long steps, double seconds) {
		this.steps = steps;
		simTime = seconds;
		// Check if any noisy events have ended.
		clearFinishedNoisyEvents();
	}

	private void logStateIfReadyAndWanted() {
		// Don't actually log yet if we are in a transaction.
		if (getTxDepth() == 0) {
			// TODO Expose item velocity.
			// TODO Check here to see if _no_ movement occurred after a time with movement.
			// TODO If so, we also want to log now.
			if (logWanted) {
				logWanted = false;
				logger.atomic(new Runnable() { @Override public void run() {

					// Log all the deferred info now.
					// Don't worry about dupe logging some of these things.

					// Meta stuff.
					logger.logSimTime(steps, simTime);
					logger.logDisplaySize(displaySize);
					logger.logView(view);
					if (tray != null) {
						logger.logTray(tray);
					}
					if (cleared) {
						logger.logClear();
						cleared = false;
					}

					// Tools.
					for (Tool tool: tools.values()) {
						everLogged.add(tool.getSoul());
						logger.logTool(tool);
						Boolean present = toolPresents.get(tool.getSoul());
						if (present != null) {
							logger.logToolPresent(tool, present);
						}
					}
					// Done using tools. Clear it out.
					tools.clear();
					toolPresents.clear();

					// Items (other than tools).
					for (Item item: items.values()) {
						everLogged.add(item.getSoul());
						logger.logItem(item);
					}
					items.clear();

					// Releases and grasps.
					for (Release release: releases.values()) {
						logger.logRelease(release.tool, release.item);
					}
					releases.clear();
					for (Grasp grasp: grasps.values()) {
						logger.logGrasp(grasp.tool, grasp.item, grasp.pointRelItem);
					}
					grasps.clear();

					// Removals.
					for (Item item: removals) {
						logger.logRemoval(item);
					}
					removals.clear();

					// Presumably we run rarely enough that this is a good time to flush.
					logger.flush();

				}});
			}
		}
	}

	@Override
	public void logTool(Tool tool) {
		// TODO Log changes in tool mode?
		Tool old = tools.get(tool.getSoul());
		if (old != null) {
			// Log changes in tool mode, even without grasps/ungrasps.
			// Allows non-cheating, and it shouldn't often happen, anyway.
			// By non-cheating, I mean we want to _discover_ relations, not have them prepackaged.
			// A grasp or ungrasp is a relation. A press or unpress is specific just to the tool.
			if (old.getMode() != tool.getMode()) {
				// These are clean events, not noisy ones.
				requestLog();
			}
		}
		tools.put(tool.getSoul(), tool.clone());
	}

	@Override
	public void logToolPresent(Tool tool, boolean toolPresent) {
		logTool(tool);
		toolPresents.put(tool.getSoul(), toolPresent);
	}

	@Override
	public void logTray(Tray tray) {
		// TODO Support clone on tray sometime?
		this.tray = tray;
	}

	@Override
	public void logView(Rectangle2D view) {
		this.view.setRect(view);
	}

	/**
	 * Says a log is wanted and will actually perform the log if we aren't in a
	 * transaction.
	 */
	private void requestLog() {
		logWanted = true;
		logStateIfReadyAndWanted();
	}

	/**
	 * Event tracking can blur on and off. We could smooth values across time at
	 * a lower level, but for now, just lump everything together and smooth at
	 * the high (even boolean level).
	 */
	private void trackNoisyEvent(Soul soul) {
		boolean doLog = true;
		if (noisyEventTimes.containsKey(soul)) {
			// We've seen this item move too recently for this to be interesting.
			doLog = false;
		} else {
			EVENTS: for (double eventTime: noisyEventTimes.values()) {
				if (simTime - eventTime <= BUSY_NOISE_DURATION) {
					// Things have just been too busy recently.
					doLog = false;
					break EVENTS;
				}
			}
		}
		if (doLog) {
			requestLog();
			//System.out.println('.');
		}
		// Remember the last time we saw this thing in motion.
		noisyEventTimes.put(soul, simTime);
	}

}
