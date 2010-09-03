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

	private static class ItemInfo {
		// Id not actually held in the item.
		public int id;
		// Duped info to compare for changes.
		public Block item;
	}

	Point2D displaySize = new Point2D.Double();

	boolean firstPerTx;

	private Map<Block, ItemInfo> items = new HashMap<Block, ItemInfo>();

	private int nextId = 1;

	Point2D toolPoint = new Point2D.Double();

	private long startTime;

	private long time;

	private boolean toolPresent;

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
			FileOutputStream out = new FileOutputStream(new File(dir, logName));
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
			info.id = nextId++;
			info.item = new Block();
			items.put(item, info);
			Point2D extent = item.getExtent();
			log("item %d", info.id);
			log("type %d box", info.id, extent.getX(), extent.getY());
			log("extent %d %.3f %.3f", info.id, extent.getX(), extent.getY());
			float[] color = item.getColor().getRGBComponents(null);
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
		if (!displaySize.equals(size)) {
			displaySize.setLocation(size);
			// Extent is no good since no guarantee of even/odd.
			// Therefore size.
			log("size display %d %d", (int)size.getX(), (int)size.getY());
		}
	}

	public void logGrasp(final Block item, final Point2D pointRelItem) {
		atomic(new Runnable() { @Override public void run() {
			ItemInfo info = getInfo(item);
			log("grasp 0 %d %.3f %.3f", info.id, pointRelItem.getX(), pointRelItem.getY());
		}});
	}

	public void logItem(final Block item) {
		atomic(new Runnable() { @Override public void run() {
			ItemInfo info = getInfo(item);
			// TODO Could check for changes in color or shape here, too.
			// Alive: alive.
			if (item.isAlive() != info.item.isAlive()) {
				info.item.setAlive(true);
				log("alive %d %s", info.id, item.isAlive());
			}
			// Position: pos.
			Point2D position = item.getPosition();
			if (!position.equals(info.item.getPosition())) {
				info.item.setPosition(position.getX(), position.getY());
				log("pos %d %.3f %.3f", info.id, position.getX(), position.getY());
			}
			// Angle: rot.
			double angle = item.getAngle();
			if (Math.abs(angle - info.item.getAngle()) > 0.001) {
				info.item.setAngle(angle);
				log("rot %d %.3f", info.id, angle);
			}
		}});
	}

	public void logMove(Point2D point) {
		if (!point.equals(toolPoint)) {
			log("pos 0 %.3f %.3f", point.getX(), point.getY());
			toolPoint.setLocation(point);
		}
	}

	public void logRelease(final Block item) {
		atomic(new Runnable() { @Override public void run() {
			ItemInfo info = getInfo(item);
			log("release 0 %d", info.id);
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
			log("item -1");
			log("type -1 view");
			// Hardcoded info about the mouse pointer.
			log("item 0");
			log("type 0 tool");
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

	public void logToolPresent(boolean toolPresent) {
		// TODO Develop general "state" class(es) for duplication purposes such as here in logger.
		// TODO The current mechanism is ad hoc.
		if (toolPresent != this.toolPresent) {
			this.toolPresent = toolPresent;
			log("present 0 %s", toolPresent);
		}
	}

	public void logView(final Rectangle2D view) {
		if (!view.equals(this.view)) {
			atomic(new Runnable() { @Override public void run() {
				Point2D extent = extent(view);
				if (!extent.equals(extent(Logger.this.view))) {
					log("extent -1 %.3f %.3f", extent.getX(), extent.getY());
				}
				Point2D center = center(view);
				if (!center.equals(center(Logger.this.view))) {
					log("pos -1 %.3f %.3f", center.getX(), center.getY());
				}
				Logger.this.view.setRect(view);
			}});
		}
	}

}
