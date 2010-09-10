package stackiter.sim;

import static stackiter.sim.Util.*;

import java.awt.geom.*;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Loggers to file, ROS, Matlab, and web server all seem possible.
 */
public class Logger implements Closeable {

	/**
	 * We mostly log 3 decimal places, so if we're equal within 4, don't worry
	 * about changes.
	 */
	private static final double EPSILON = 1e-4;

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

	Point2D displaySize = new Point2D.Double();

	boolean firstPerTx;

	private int idNext = 4;

	private int idTray = 3;

	private int idView = 2;

	private int idWorld = 1;

	private Map<Block, ItemInfo> items = new HashMap<Block, ItemInfo>();

	private long startTime;

	private long time;

	private Map<Tool, ToolInfo> tools = new HashMap<Tool, ToolInfo>();

	private boolean trayLogged;

	private int txDepth;

	private Rectangle2D view = new Rectangle2D.Double();

	private Formatter writer;

	public Logger() {
		try {
			// Log start time.
			startTime = System.currentTimeMillis();
			time = startTime;
			// Log file.
			File dir = new File(System.getProperty("java.io.tmpdir"), "stackiter");
			dir.mkdirs();
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");
			String logName = String.format("stackiter-%s.log", format.format(new Date(startTime)));
			File logFile = new File(dir, logName);
			System.out.println(logFile);
			FileOutputStream out = new FileOutputStream(logFile);
			writer = new Formatter(new BufferedWriter(new OutputStreamWriter(out, "UTF-8"), 1<<16));
			// Initial info.
			logStart();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void atomic(Runnable runnable) {
		beginStep();
		try {
			runnable.run();
		} finally {
			endStep();
		}
	}

	private void beginStep() {
		if (txDepth == 0) {
			firstPerTx = true;
		}
		txDepth++;
	}

	@Override
	public void close() {
		try {
			log("quit");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void endStep() {
		txDepth--;
	}

	private ItemInfo getInfo(Block item) {
		ItemInfo info = items.get(item);
		if (info == null) {
			// New item. Log its static information.
			info = new ItemInfo();
			info.id = idNext++;
			info.item = new BasicItem();
			items.put(item, info);
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
		ToolInfo info = tools.get(tool);
		if (info == null) {
			// New item. Log its static information.
			info = new ToolInfo();
			info.id = idNext++;
			info.tool = new Tool();
			tools.put(tool, info);
			log("item %d", info.id);
			log("type %d tool", info.id);
			float[] color = tool.getColor().getRGBComponents(null);
			log("color %d %.3f %.3f %.3f %.3f", info.id, color[0], color[1], color[2], color[3]);
		}
		return info;
	}

	private void log(String message, Object... args) {
		logTimeIfNeeded();
		writer.format(message + "\n", args);
	}

	/**
	 * @param size pixel width and height as integer values, despite use of Point2D.
	 */
	public void logDisplaySize(Point2D size) {
		if (!approx(displaySize, size, EPSILON)) {
			displaySize.setLocation(size);
			// Extent is no good since no guarantee of even/odd.
			// Therefore size.
			log("size display %d %d", (int)size.getX(), (int)size.getY());
		}
	}

	public void logGrasp(final Tool tool, final Block item, final Point2D pointRelItem) {
		atomic(new Runnable() { @Override public void run() {
			ToolInfo toolInfo = getInfo(tool);
			ItemInfo info = getInfo(item);
			log("grasp %d %d %.3f %.3f", toolInfo.id, info.id, pointRelItem.getX(), pointRelItem.getY());
		}});
	}

	public void logItem(final Block item) {
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
			// Angle: rot.
			double angle = item.getAngle();
			if (!approx(angle, info.item.getAngle(), EPSILON)) {
				info.item.setAngle(angle);
				log("rot %d %.3f", info.id, angle);
			}
		}});
	}

	public void logRelease(final Tool tool, final Block item) {
		atomic(new Runnable() { @Override public void run() {
			ToolInfo toolInfo = getInfo(tool);
			ItemInfo info = getInfo(item);
			log("release %d %d", toolInfo.id, info.id);
		}});
	}

	public void logRemoval(final Block item) {
		atomic(new Runnable() { @Override public void run() {
			ItemInfo info = getInfo(item);
			log("destroy %d", info.id);
		}});
	}

	private void logStart() {
		atomic(new Runnable() { @Override public void run() {
			// Hardcoded info about the view.
			log("item %d", idView);
			log("type %d view", idView);
		}});
	}

	private void logTimeIfNeeded() {
		if (txDepth == 0 || firstPerTx) {
			firstPerTx = false;
			long currentTime = System.currentTimeMillis();
			if (currentTime != time) {
				time = currentTime;
				writer.format("time %d\n", currentTime - startTime);
			}
		}
	}

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

	/**
	 * Really, this is about mouse tools. From a UI perspective, it might be
	 * nice to know when the mouse comes or goes from the window, even if that
	 * doesn't directly affect world state.
	 */
	public void logToolPresent(Tool tool, boolean toolPresent) {
		ToolInfo info = getInfo(tool);
		if (toolPresent != info.present) {
			info.present = toolPresent;
			log("present %d %s", info.id, toolPresent);
		}
	}

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

	public void logView(final Rectangle2D view) {
		// TODO Approximate equality?
		if (!view.equals(this.view)) {
			atomic(new Runnable() { @Override public void run() {
				Point2D extent = extent(view);
				if (!extent.equals(extent(Logger.this.view))) {
					log("extent %d %.3f %.3f", idView, extent.getX(), extent.getY());
				}
				Point2D center = center(view);
				if (!center.equals(center(Logger.this.view))) {
					log("pos %d %.3f %.3f", idView, center.getX(), center.getY());
				}
				Logger.this.view.setRect(view);
			}});
		}
	}

}
