package stackiter.sim;

import static java.lang.Boolean.*;
import static java.lang.Integer.*;

import java.util.*;

/**
 * For headless batch jobs.
 *
 * TODO Standardize world/agent configuration.
 */
public class Batch implements Runnable {

	public static void main(String[] args) {
		new Batch(parseArgs(args)).run();
	}

	/**
	 * Parses args in sloppy alternating key value format, treating both as
	 * strings.
	 */
	private static Map<String, String> parseArgs(String[] args) {
		Map<String, String> map = new HashMap<String, String>();
		for (int a = 0; a < args.length; a++) {
			// Note the post-increment here.
			String key = args[a++];
			// Validate.
			if (map.containsKey(key)) {
				throw new RuntimeException("Duplicate key: " + key);
			}
			if (a >= args.length) {
				throw new RuntimeException("No value for key: " + key);
			}
			// Good to store the value.
			String value = args[a];
			map.put(key, value);
		}
		return map;
	}

	private Map<String, String> args;

	/**
	 * All keys and values must be non-null.
	 * Otherwise, behavior is undefined.
	 */
	public Batch(Map<String, String> args) {
		this.args = args;
	}

	private String arg(String key, String defaultValue) {
		String value = args.get(key);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

	@Override
	public void run() {
		World world = new World();

		// Arguments.
		// Episode limit.
		int episodeLimit = parseInt(arg("episode-limit", "100"));
		// Scenario.
		String scenario = arg("scenario", "default");
		if (scenario.equals("default")) {
			// Working directly from Stackiter.initScenarios allows manual
			// verification to match kick-offs here.
			Stackiter.initScenarios(world);
		} else {
			// TODO Handle other cases as needed.
		}

		// Logger.
		//Logger logger = new FilterLogger(new TextLogger());
		//Logger logger = new TextLogger();
		String logDir = arg("log-dir", "");
		boolean compressLog = parseBoolean(arg("compress-log", "true"));
		Logger logger = new EpisodicLogger(new TextLogger(logDir, compressLog));
		logger.waitForEpisodeStart();
		world.setLogger(logger);

		// Main loop.
		long steps = 0;
		long stepsPerSecond = 100;
		long stepsPerMinute = 60 * stepsPerSecond;
		long stepsPerHour = 60 * stepsPerMinute;
		//while (world.getSimTime() < stepsPerHour / stepsPerSecond) {
		while (world.getClearCount() < episodeLimit) {
			try {
				world.update();
			} catch (Exception e) {
				// TODO Remove this handler?
				// TODO Better or worse to keep on trucking?
				e.printStackTrace();
			}
			// Simple status.
			steps++;
			if (steps % stepsPerMinute == 0) {
				System.out.print(".");
			}
			if (steps % stepsPerHour == 0) {
				System.out.printf(" Hours: %d\n", steps/stepsPerHour);
			}
		}
		logger.close();
	}

}
