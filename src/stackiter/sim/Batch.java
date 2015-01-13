package stackiter.sim;

import static java.lang.Boolean.*;
import static java.lang.Integer.*;
import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import javax.swing.*;

import stackiter.tasks.*;

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

	private JComponent display;

	private Map<String, Iterable<Scenario>> scenariosMap;

	private World world;

	/**
	 * All keys and values must be non-null.
	 * Otherwise, behavior is undefined.
	 */
	public Batch(Map<String, String> args) {
		this.args = args;
		initScenarios();
	}

	private String arg(String key, String defaultValue) {
		String value = args.get(key);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

	@SuppressWarnings("serial")
	private void initDisplay() {
		JFrame frame = new JFrame("Stackiter Batch Display");
		frame.setLayout(new BorderLayout());
		display = new JComponent() {
			@Override
			protected void paintComponent(Graphics graphics) {
				Graphics2D g = copy(graphics);
				try {
					AffineTransform transform = worldToDisplayTransform();
					g.transform(transform);
					world.paintItems(g);
				} finally {
					g.dispose();
				}
			}
			private AffineTransform worldToDisplayTransform() {
				AffineTransform transform = new AffineTransform();
				transform.translate(0.5 * getWidth(), getHeight());
				double scale = 5.0;
				transform.scale(scale, -scale);
				return transform;
			}
		};
		frame.add(display, BorderLayout.CENTER);
		frame.setSize(600, 400);
		frame.setVisible(true);
	}

	private void initScenarios() {
		scenariosMap = new LinkedHashMap<String, Iterable<Scenario>>();
		scenariosMap.put("external", Arrays.asList(
			// First, so the others can respond.
			new Scenario.WideTable(),
			// Others.
			new Scenario.ExternalControl(),
			new Scenario.Refill()
		));
	}

	@Override
	public void run() {
		world = new World();

		// Logger.
		//Logger logger = new FilterLogger(new TextLogger());
		//Logger logger = new TextLogger();
		String logDir = arg("log-dir", "");
		String logSuffix = arg("log-suffix", "");
		boolean compressLog = parseBoolean(arg("compress-log", "true"));
		Logger logger =
			new EpisodicLogger(new TextLogger(logDir, logSuffix, compressLog));
		// Display.
		final boolean doDisplay = false; // TODO parse arg!
		if (doDisplay) initDisplay();
		try {
			logger.waitForEpisodeStart();

			// Scenario.
			// The arg is singular for now, to simply the interface, but we
			// build a list from it.
			// TODO Allow a list of some sort?
			Iterable<Scenario> scenarios;
			String scenarioName = arg("scenario", "default");
			if (scenarioName.equals("default")) {
				// Working directly from Stackiter.initScenarios allows manual
				// verification to match kick-offs here.
				scenarios = Stackiter.defaultScenarios();
			} else {
				scenarios = scenariosMap.get(scenarioName);
				if (scenarios == null) {
					throw new RuntimeException(
						"Unknown scenario: " + scenarioName
					);
				}
			}
			// Get it going.
			Scenario.handleWorldSetup(scenarios, world, logger);

			// Episode limit.
			final int episodeLimit = parseInt(arg("episode-limit", "100"));

			// Main loop.
			long steps = 0;
			long stepsPerSecond = 100;
			long stepsPerMinute = 60 * stepsPerSecond;
			long stepsPerHour = 60 * stepsPerMinute;
			//while (world.getSimTime() < stepsPerHour / stepsPerSecond) {
			while (
				world.getClearCount() < episodeLimit && !world.isQuitRequested()
			) {
				try {
					world.update();
				} catch (Exception e) {
					// TODO Remove this handler?
					// TODO Better or worse to keep on trucking?
					e.printStackTrace();
				}
				if (doDisplay) display.repaint();
				// Simple status.
				steps++;
				if (steps % stepsPerMinute == 0) {
					System.out.print(".");
				}
				if (steps % stepsPerHour == 0) {
					System.out.printf(
						" Hours: %d\n", steps/stepsPerHour
					);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			logger.close();
			if (doDisplay) {
				// Kill AWT/Swing threads.
				System.exit(0);
			}
		}
	}

}
