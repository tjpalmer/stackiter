package stackiter;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;

/**
 * Loggers to file, ROS, Matlab, and web server all seem possible.
 */
public class Logger implements Closeable {

	private boolean first = true;

	private Map<Block, Integer> items = new HashMap<Block, Integer>();

	private long startTime;

	private long time;

	private Random random = new Random();

	private Formatter writer;

	public Logger() {
		try {
			FileOutputStream out = new FileOutputStream(File.createTempFile("stackiter-", ".log"));
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
		Integer id = items.get(item);
		if (id == null) {
			// New item. Log its static information.
			id = random.nextInt();
			items.put(item, id);
			Point2D extent = item.getExtent();
			log("item %d", id, extent.getX(), extent.getY());
			log("shape %d box %f %f", id, extent.getX(), extent.getY());
			Color color = item.getColor();
			float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
			log("color %d hsb %f %f %f", id, hsb[0], hsb[1], hsb[2]);
		}
		Point2D position = item.getPosition();
		log("position %d xy %f %f", id, position.getX(), position.getY());
	}

	private void logTimeIfNeeded() {
		if (first) {
			startTime = System.currentTimeMillis();
			time = startTime;
		}
		long currentTime = System.currentTimeMillis();
		if (currentTime != time || first) {
			writer.format("time ms %d\n", time - startTime);
		}
		time = currentTime;
		first = false;
	}

}
