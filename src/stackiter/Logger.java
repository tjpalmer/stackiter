package stackiter;

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

	boolean firstPerTx;

	private Map<Block, ItemInfo> items = new HashMap<Block, ItemInfo>();

	private int nextId = 1;

	private long startTime;

	private long time;

	private int txDepth;

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
			if (item.isAlive()) {
				log("alive %d", info.id);
			}
			log("shape %d box %f %f", info.id, extent.getX(), extent.getY());
			log("color %d %x", info.id, item.getColor().getRGB());
		}
		return info;
	}

	private void log(String message, Object... args) {
		logTimeIfNeeded();
		writer.format(message + "\n", args);
	}

	public void logEnter() {
		log("enter");
	}

	public void logGrasp(final Block item, final Point2D pointRelItem) {
		atomic(new Runnable() { @Override public void run() {
			ItemInfo info = getInfo(item);
			log("grasp %d %f %f", info.id, pointRelItem.getX(), pointRelItem.getY());
		}});
	}

	public void logItem(final Block item) {
		atomic(new Runnable() { @Override public void run() {
			ItemInfo info = getInfo(item);
			// TODO Could check for changes in alive (if datafied), color, or shape here, too.
			Point2D position = item.getPosition();
			if (!position.equals(info.item.getPosition())) {
				info.item.setPosition(position.getX(), position.getY());
				log("pos %d %f %f", info.id, position.getX(), position.getY());
			}
		}});
	}

	public void logLeave() {
		log("leave");
	}

	public void logMove(Point2D point) {
		// TODO Consider checking for changes even though that's mostly handled outside.
		log("move %f %f", point.getX(), point.getY());
	}

	public void logRelease(final Block item) {
		atomic(new Runnable() { @Override public void run() {
			ItemInfo info = getInfo(item);
			log("release %d", info.id);
		}});
	}

	public void logRemoval(final Block item) {
		atomic(new Runnable() { @Override public void run() {
			ItemInfo info = getInfo(item);
			log("destroy %d", info.id);
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

}
