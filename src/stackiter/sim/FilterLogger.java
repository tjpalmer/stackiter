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

	private Map<Object, Release> releases = new LinkedHashMap<Object, Release>();

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
		}
	}

	@Override
	public void logSimTime(long steps, double seconds) {
		this.steps = steps;
		simTime = seconds;
	}

	private void logStateIfReadyAndWanted() {
		// Don't actually log yet if we are in a transaction.
		if (getTxDepth() == 0) {
			// TODO Check here to see if _no_ movement occurred after a time with movement.
			// TODO Expose item velocity.
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

}
