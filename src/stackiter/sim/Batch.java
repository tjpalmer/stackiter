package stackiter.sim;

/**
 * For headless batch jobs.
 *
 * TODO Standardize world/agent configuration.
 */
public class Batch {

	public static void main(String[] args) {
		World world = new World();
		Logger logger = new EpisodicLogger(new TextLogger());
		logger.waitForEpisodeStart();
		//Logger logger = new FilterLogger(new TextLogger());
		//Logger logger = new TextLogger();
		Stackiter.initScenarios(world);
		world.setLogger(logger);
		long steps = 0;
		long stepsPerSecond = 100;
		long stepsPerMinute = 60 * stepsPerSecond;
		long stepsPerHour = 60 * stepsPerMinute;
		//while (world.getSimTime() < stepsPerHour / stepsPerSecond) {
		while (world.getClearCount() < 1000) {
			world.update();
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
