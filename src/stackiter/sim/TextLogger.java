package stackiter.sim;

import static java.util.Map.*;
import static java.lang.String.*;
import static stackiter.sim.Util.*;

import java.awt.geom.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

/**
 * Logs in text format to a temp file.
 */
public class TextLogger extends AtomicLogger {

	/**
	 * Opens a log file for writing in the given directory, compressed if
	 * requested.
	 * The file name is also based on the current date and time.
	 * The suffix, if present, gets a "-" prepended before it, and it goes
	 * before the extension.
	 */
	public static Formatter openLogFile(
		String logDir, String suffix, boolean doCompress
	) {
		try {
			// Log file.
			if (logDir == null || logDir.isEmpty()) {
				logDir =
					System.getProperty("java.io.tmpdir") + File.separator +
						"stackiter";
			}
			File dir = new File(logDir);
			dir.mkdirs();
			SimpleDateFormat format =
				new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");
			if (suffix == null || suffix.isEmpty()) {
				suffix = "";
			} else {
				suffix = "-" + suffix;
			}
			String logName = String.format(
				"stackiter-%s%s.log", format.format(new Date()), suffix
			);
			if (doCompress) {
				logName += ".gz";
			}
			File logFile = new File(dir, logName);
			// TODO What's the right way to expose the log file name?
			System.out.println(logFile);
			OutputStream out = new FileOutputStream(logFile);
			try {
				if (doCompress) {
					out = new GZIPOutputStream(out);
				}
				return new Formatter(new BufferedWriter(
					// I think I made the buffer large in hopes for speed.
					new OutputStreamWriter(out, "UTF-8"), 1 << 16
				));
			} catch (Exception e) {
				out.close();
				throw e;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static class ItemInfo {
		// Id not actually held in the item.
		public int id;
		// Duped info to compare for changes.
		public Item item;
	}

	private static class ToolInfo {
		// Id not actually held in the item.
		public int id;
		// Only meaningful for mouse tools, so far, but here it is.
		public boolean present;
		// Duped info to compare for changes.
		public Tool tool;
	}

	/**
	 * We mostly log 3 decimal places, so if we're equal within 4, don't worry
	 * about changes.
	 */
	private static final double EPSILON = 1e-4;

	Point2D displaySize = new Point2D.Double();

	private boolean doFlush;

	boolean firstPerTx;

	private int idNext = 4;

	private int idTray = 3;

	private int idView = 2;

	private int idWorld = 1;

	private Map<Soul, ItemInfo> items = new HashMap<Soul, ItemInfo>();

	private double simTime;

	private long startTime;

	private long steps;

	private long time;

	private Map<Soul, ToolInfo> tools = new HashMap<Soul, ToolInfo>();

	private boolean trayLogged;

	private Rectangle2D view = new Rectangle2D.Double();

	private List<Formatter> writers;

	public TextLogger() {
		this("");
	}

	public TextLogger(String logDir) {
		this(logDir, true);
	}

	public TextLogger(String logDir, String suffix) {
		this(logDir, suffix, true);
	}

	public TextLogger(String logDir, boolean doCompress) {
		// Ideally we pass in the official start time, but this will do.
		this(logDir, "", doCompress);
	}

	public TextLogger(String logDir, String suffix, boolean doCompress) {
		// Ideally we pass in the official start time, but this will do.
		this(openLogFile(logDir, suffix, doCompress));
	}

	public TextLogger(Formatter... formatters) {
		// Actual output.
		writers = new ArrayList<Formatter>(Arrays.asList(formatters));
		// Start time of our log.
		startTime = System.currentTimeMillis();
		time = startTime;
		// Initial info.
		logStart();
	}

	public void addOutput(Formatter formatter) {
		// TODO Instead use a TeeWriter or some such inside the formatter?
		writers.add(formatter);
	}

	@Override
	protected void beginStep() {
		if (getTxDepth() == 0) {
			firstPerTx = true;
		}
		super.beginStep();
	}

	@Override
	public void close() {
		try {
			log("quit");
			for (Formatter writer: writers) {
				writer.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void endStep() {
		super.endStep();
		if (doFlush) {
			doFlush = false;
			flushDirect();
		}
	}

	@Override
	public void flush() {
		if (getTxDepth() == 0) {
			flushDirect();
		} else {
			doFlush = true;
		}
	}

	private void flushDirect() {
		for (Formatter writer: writers) {
			writer.flush();
		}
	}

	private ItemInfo getInfo(Item item) {
		ItemInfo info = items.get(item.getSoul());
		if (info == null) {
			// New item. Log its static information.
			info = new ItemInfo();
			info.id = idNext++;
			info.item = new BasicItem();
			items.put(item.getSoul(), info);
			Point2D extent = item.getExtent();
			log("item %d", info.id);
			log("type %d box", info.id);
			log("extent %d %.3f %.3f", info.id, extent.getX(), extent.getY());
			float[] color = item.getColor().getRGBComponents(null);
			log("color %d %.3f %.3f %.3f %.3f", info.id, color[0], color[1], color[2], color[3]);
			if (!item.isAlive()) {
				// Presume all prebirth items are in the tray frame.
				log("rel %d %d", info.id, idTray);
			}
		}
		return info;
	}

	private ToolInfo getInfo(Tool tool) {
		// Use the souls for keys, because those should stay the same across copies.
		ToolInfo info = tools.get(tool.getSoul());
		if (info == null) {
			// New item. Log its static information.
			info = new ToolInfo();
			info.id = idNext++;
			info.tool = new Tool();
			tools.put(tool.getSoul(), info);
			log("item %d", info.id);
			log("type %d tool", info.id);
			float[] color = tool.getColor().getRGBComponents(null);
			log("color %d %.3f %.3f %.3f %.3f", info.id, color[0], color[1], color[2], color[3]);
		}
		return info;
	}

	/**
	 * Returns the soul of the object corresponding the given id.
	 *
	 * If the id isn't found or if there's no corresponding soul, returns null.
	 */
	public Soul getSoul(int id) {
		// TODO Maintain a map?
		// Items.
		for (Entry<Soul, ItemInfo> entry: items.entrySet()) {
			if (entry.getValue().id == id) {
				return entry.getKey();
			}
		}
		// Tools.
		for (Entry<Soul, ToolInfo> entry: tools.entrySet()) {
			if (entry.getValue().id == id) {
				return entry.getKey();
			}
		}
		// Nothing.
		// TODO Can we provide souls for anything else?
		return null;
	}

	private void log(String message, Object... args) {
		logTimeIfNeeded();
		logDirect(message, args);
	}

	@Override
	public void logClear() {
		log("clear");
	}

	/**
	 * Directly log out to the writer.
	 */
	private void logDirect(String message, Object... args) {
		for (Formatter writer: writers) {
			writer.format(message + "\n", args);
		}
	}

	@Override
	public void logDisplaySize(Point2D size) {
		if (!approx(displaySize, size, EPSILON)) {
			displaySize.setLocation(size);
			// Extent is no good since no guarantee of even/odd.
			// Therefore size.
			log("size display %d %d", (int)size.getX(), (int)size.getY());
		}
	}

	@Override
	public void logGrasp(final Tool tool, final Block item, final Point2D pointRelItem) {
		atomic(new Runnable() { @Override public void run() {
			ToolInfo toolInfo = getInfo(tool);
			ItemInfo info = getInfo(item);
			log("grasp %d %d %.3f %.3f", toolInfo.id, info.id, pointRelItem.getX(), pointRelItem.getY());
		}});
	}

	@Override
	public void logItem(final Item item) {
		atomic(new Runnable() { @Override public void run() {
			ItemInfo info = getInfo(item);
			// TODO Could check for changes in color or shape here, too.
			// Alive: alive.
			if (item.isAlive() != info.item.isAlive()) {
				info.item.setAlive(item.isAlive());
				if (item.isAlive()) {
					// Presume all newly alive items are suddenly in the world frame.
					log("rel %d %d", info.id, idWorld);
				}
				log("alive %d %s", info.id, item.isAlive());
			}
			// Position: pos.
			Point2D position = item.getPosition();
			if (!approx(position, info.item.getPosition(), EPSILON)) {
				info.item.setPosition(position);
				log("pos %d %.3f %.3f", info.id, position.getX(), position.getY());
			}
			// Linear velocity: posvel.
			// Velocities matter for logging if we don't log every frame.
			// We could log acceleration, too, since we use that (or jerk) for key state detection.
			// However, we detect up front today before logging. If we changed that to after-the-fact detection,
			// we'd be logging full amounts anyway and be able to calculate it.
			// I think I'll likely only use velocity for now in the actual concept learning?
			// TODO Do log acceleration if we decide we need it for concept learning.
			Point2D linearVelocity = item.getLinearVelocity();
			if (!approx(linearVelocity, info.item.getLinearVelocity(), EPSILON)) {
				info.item.setLinearVelocity(linearVelocity);
				log("posvel %d %.3f %.3f", info.id, linearVelocity.getX(), linearVelocity.getY());
			}
			// Angle: rot.
			double angle = item.getAngle();
			if (!approx(angle, info.item.getAngle(), EPSILON)) {
				info.item.setAngle(angle);
				log("rot %d %.3f", info.id, angle);
			}
			// Angular velocity: rotvel.
			double angularVelocity = item.getAngularVelocity();
			if (!approx(angularVelocity, info.item.getAngularVelocity(), EPSILON)) {
				info.item.setAngularVelocity(angularVelocity);
				log("rotvel %d %.3f", info.id, angularVelocity);
			}
		}});
	}

	@Override
	public void logMeta(final Meta meta) {
		atomic(new Runnable() { @Override public void run() {
			// Need a loop to build args text because the quantity is variable.
			StringBuilder args = new StringBuilder();
			for (Object arg: meta.args) {
				if (arg instanceof Point2D) {
					// Wrapping in parens is clearer, but unwrapped looks more
					// like other log lines.
					// TODO Okay to leave implied that this is a single arg?
					Point2D point = (Point2D)arg;
					args.append(format(
						" %.3f %.3f", point.getX(), point.getY()
					));
				} else if (arg instanceof Soul) {
					ItemInfo item = items.get(arg);
					if (item == null) {
						throw new RuntimeException(
							arg + " not in " + items.keySet()
						);
					}
					args.append(" " + item.id);
				} else if (arg == null) {
					args.append(" null");
				} else {
					throw new RuntimeException(
						"Only points and item souls for now, not " +
							arg.getClass()
					);
				}
			}
			log("meta %s%s", meta.name, args);
		}});
	}

	@Override
	public void logRelease(final Tool tool, final Block item) {
		atomic(new Runnable() { @Override public void run() {
			ToolInfo toolInfo = getInfo(tool);
			ItemInfo info = getInfo(item);
			log("release %d %d", toolInfo.id, info.id);
		}});
	}

	@Override
	public void logRemoval(final Item item) {
		atomic(new Runnable() { @Override public void run() {
			ItemInfo info = getInfo(item);
			log("destroy %d", info.id);
		}});
	}

	@Override
	public void logSimTime(long steps, double seconds) {
		if (simTime != seconds || steps != this.steps) {
			simTime = seconds;
			this.steps = steps;
			log("time sim %d %.3f", steps, seconds);
		}
	}

	private void logStart() {
		atomic(new Runnable() { @Override public void run() {
			// Hardcoded info about the view.
			// TODO Don't hardcode this! Let someone else tell us.
			log("item %d", idView);
			log("type %d view", idView);
		}});
	}

	private void logTimeIfNeeded() {
		if (getTxDepth() == 0 || firstPerTx) {
			firstPerTx = false;
			long currentTime = System.currentTimeMillis();
			if (currentTime != time) {
				time = currentTime;
				double realSeconds = 1e-3 * (currentTime - startTime);
				logDirect("time real %.3f", realSeconds);
			}
		}
	}

	@Override
	public void logTool(final Tool tool) {
		atomic(new Runnable() { @Override public void run() {
			ToolInfo info = getInfo(tool);
			// Position: pos.
			Point2D position = tool.getPosition();
			if (!approx(position, info.tool.getPosition(), EPSILON)) {
				info.tool.setPosition(position);
				log("pos %d %.3f %.3f", info.id, position.getX(), position.getY());
			}
			// Mode: pressed.
			if (tool.getMode() != info.tool.getMode()) {
				// TODO Could log the actual mode, but I'm not convinced I'm ever going to care past binary.
				info.tool.setMode(tool.getMode());
				log("pressed %d %s", info.id, tool.getMode() != ToolMode.INACTIVE);
			}
		}});
	}

	@Override
	public void logToolPresent(Tool tool, boolean toolPresent) {
		ToolInfo info = getInfo(tool);
		if (toolPresent != info.present) {
			info.present = toolPresent;
			log("present %d %s", info.id, toolPresent);
		}
	}

	@Override
	public void logTray(Tray tray) {
		if (!trayLogged) {
			// All this assumes that the tray never changes.
			trayLogged = true;
			log("item %d", idTray);
			log("type %d tray", idTray);
			Point2D pos = tray.getAnchor();
			if (tray.isFixedToDisplay()) {
				// Default is relative to world, so only give rel if to view.
				log("rel %d %d", idTray, idView);
				pos = point(pos.getX() - view.getMinX(), pos.getY() - view.getMinY());
			}
			log("pos %d %.3f %.3f", idTray, pos.getX(), pos.getY());
		}
	}

	@Override
	public void logView(final Rectangle2D view) {
		// TODO Approximate equality?
		if (!view.equals(this.view)) {
			atomic(new Runnable() { @Override public void run() {
				Point2D extent = extent(view);
				if (!extent.equals(extent(TextLogger.this.view))) {
					log("extent %d %.3f %.3f", idView, extent.getX(), extent.getY());
				}
				Point2D center = center(view);
				if (!center.equals(center(TextLogger.this.view))) {
					log("pos %d %.3f %.3f", idView, center.getX(), center.getY());
				}
				TextLogger.this.view.setRect(view);
			}});
		}
	}

}
