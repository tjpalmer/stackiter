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

	private Map<Block, ItemInfo> items = new HashMap<Block, ItemInfo>();

	private int nextId = 1;

	private long startTime;

	private long time;

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
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		try {
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void log(String message, Object... args) {
		logTimeIfNeeded();
		writer.format(message + "\n", args);
	}

	public void logItem(Block item) {
		ItemInfo info = items.get(item);
		if (info == null) {
			// New item. Log its static information.
			info = new ItemInfo();
			info.id = nextId++;
			info.item = new Block();
			items.put(item, info);
			Point2D extent = item.getExtent();
			log("item %d", info.id);
			log("shape %d box %f %f", info.id, extent.getX(), extent.getY());
			log("color %d %x", info.id, item.getColor().getRGB());
		}
		// TODO Could check for changes in shape or color here, too.
		Point2D position = item.getPosition();
		if (!position.equals(info.item.getPosition())) {
			info.item.setPosition(position.getX(), position.getY());
			log("pos %d %f %f", info.id, position.getX(), position.getY());
		}
	}

	public void logRemoval(Block item) {
		ItemInfo info = items.get(item);
		log("destroy %d", info.id);
	}

	private void logTimeIfNeeded() {
		long currentTime = System.currentTimeMillis();
		if (currentTime != time) {
			time = currentTime;
			writer.format("time %d\n", currentTime - startTime);
		}
	}

}
