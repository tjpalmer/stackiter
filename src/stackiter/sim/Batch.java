package stackiter.sim;

import static java.lang.Boolean.*;
import static java.lang.Integer.*;
import static java.lang.Math.*;
import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;
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

	private int frameIndex = 0;

	private double lastClearCount = -1;

	private double lastDisplayTranslate = 0.0;

	private double lastDisplayTranslateGoal = 0.0;

	private String logDir;

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
				paintFrame(graphics, getSize());
			}
		};
		frame.add(display, BorderLayout.CENTER);
		frame.setSize(600, 400);
		frame.setVisible(true);
	}

	private void initScenarios() {
		scenariosMap = new LinkedHashMap<String, Iterable<Scenario>>();
		// TODO Unify this handling somewhat.
		scenariosMap.put("external", Arrays.asList(
			// First, so the others can respond.
			new Scenario.WideTable(),
			// Others.
			new Scenario.ExternalControl(),
			new Scenario.Refill()
		));
		scenariosMap.put("external-arch", Arrays.asList(
			// First, so the others can respond.
			new Scenario.WideTable(),
			// Others.
			new Scenario.ExternalControl(),
			new Scenario.ArchRefill()
		));
	}

	private void paintFrame(Graphics graphics, Dimension size) {
		paintFrame(graphics, size, 1.0);
	}

	private void paintFrame(Graphics graphics, Dimension size, double scale) {
		Graphics2D g = copy(graphics);
		try {
			AffineTransform transform = worldToFrameTransform(size);
			// Additional custom scale.
			transform.scale(scale, scale);
			g.transform(transform);
			if (true) {
				// Center on blocks if outside display bounds.
				transform.translate(lastDisplayTranslateGoal, 0);
				Rectangle2D displayBounds = applied(
					transform.createInverse(),
					new Rectangle2D.Double(
						0, 0, size.getWidth(), size.getHeight()
					)
				);
				double minX = world.getGround().getExtent().getX();
				double maxX = -minX;
				for (Item item: world.getItems()) {
					if (item.getPosition().getY() >= 0) {
						Rectangle2D bounds =
							applied(item.getTransform(), item.getBounds());
						minX = Math.min(minX, bounds.getMinX());
						maxX = Math.max(maxX, bounds.getMaxX());
					}
				}
				double midX = (minX + maxX) / 2;
				if (
					minX < displayBounds.getMinX() ||
					maxX > displayBounds.getMaxX()
				) {
					// New goal.
					lastDisplayTranslateGoal = -midX;
				}
				double translateError =
					lastDisplayTranslateGoal - lastDisplayTranslate;
				if (abs(translateError) > 1e-2) {
					if (lastClearCount != world.getClearCount()) {
						// New episode. Just jump.
						lastDisplayTranslate = lastDisplayTranslateGoal;
					} else {
						// Pan over gradually to maintain context.
						double step = 0.1;
						if (abs(translateError) < step) {
							step = abs(translateError);
						}
						lastDisplayTranslate += signum(translateError) * step;
					}
					lastClearCount = world.getClearCount();
				}
				g.translate(lastDisplayTranslate, 0);
				// Show key x coords for debugging.
				boolean showMarkers = false;
				if (showMarkers) {
					// Display bounds based on old translate goal.
					g.setColor(Color.BLUE);
					g.draw(displayBounds);
					double top = displayBounds.getMaxY();
					// X origin.
					g.setColor(Color.BLACK);
					g.draw(new Line2D.Double(0, 0, 0, top));
					// Relative to blocks.
					g.setColor(Color.RED);
					g.draw(new Line2D.Double(minX, 0, minX, top));
					g.setColor(Color.GREEN);
					g.draw(new Line2D.Double(maxX, 0, maxX, top));
					g.setColor(Color.ORANGE);
					g.draw(new Line2D.Double(midX, 0, midX, top));
					// Translate goals.
					g.setColor(Color.MAGENTA);
					g.draw(new Line2D.Double(
						-lastDisplayTranslateGoal, 0,
						-lastDisplayTranslateGoal, top
					));
					g.setColor(Color.PINK);
					g.draw(new Line2D.Double(
						-lastDisplayTranslate, 0,
						// Only part-way up, so we can see when they align.
						-lastDisplayTranslate, top / 2
					));
				}
			}
			world.paint(g);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			g.dispose();
		}
	}

	@Override
	public void run() {
		world = new World();

		// Logger.
		//Logger logger = new FilterLogger(new TextLogger());
		//Logger logger = new TextLogger();
		logDir = arg("log-dir", "");
		String logSuffix = arg("log-suffix", "");
		boolean compressLog = parseBoolean(arg("compress-log", "true"));
		Logger logger =
			new EpisodicLogger(new TextLogger(logDir, logSuffix, compressLog));
		// Display.
		boolean doDisplay = parseBoolean(arg("display", "false"));
		boolean saveFrames = parseBoolean(arg("save-frames", "false"));
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
				if (saveFrames && world.getSimSteps() % 4 == 0) saveFrame();
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

	private void saveFrame() {
		BufferedImage image =
			new BufferedImage(320, 200, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			graphics.setBackground(Color.WHITE);
			graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
			Dimension size = new Dimension(image.getWidth(), image.getHeight());
			paintFrame(graphics, size, 0.5);
			String name = String.format("frame%05d.png", frameIndex);
			File file = new File(logDir, name);
			ImageIO.write(image, "png", file);
			frameIndex++;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			graphics.dispose();
		}
	}

	private AffineTransform worldToFrameTransform(Dimension size) {
		AffineTransform transform = new AffineTransform();
		transform.translate(0.5 * size.getWidth(), size.getHeight());
		// Ideally, we just know what scale to use, but this at least
		// seems to be a sane one for common cases.
		double scale = 10.0;
		transform.scale(scale, -scale);
		return transform;
	}

}
