package stackiter.tasks;

import java.util.*;

import stackiter.agents.*;
import stackiter.sim.*;

/**
 * Allow an external controller (such as across a socket) to control an agent.
 */
public class ExternalOptionAgent implements OptionAgent {

	private Formatter formatter;

	/**
	 * Our options factory.
	 */
	private Options options;

	private World world;

	public ExternalOptionAgent(World world) {
		this.world = world;
		formatter = new Formatter(System.out);
		options = new Options(world.getTray().getRandom());
	}

	@Override
	public Option act(State state) {
		// Presumably the logger will flush things anyway, but just for good
		// measure ...
		formatter.flush();
		// TODO Support state output to external controller?
		// TODO Change to socket or support console option?
		String command = System.console().readLine();
		List<String> args = Arrays.asList(command.trim().split("\\s+"));
		// Now call the command the first token.
		command = args.isEmpty() ? "" : args.get(0);
		if (command.equals("quit")) {
			world.requestQuit();
		}
		// TODO Do something with the command.
		return options.delay();
	}

	public void listen(Logger logger) {
		// Get the deepest logger, presuming it's a TextLogger.
		TextLogger textLogger = null;
		while (true) {
			Logger kid = logger.getKid();
			if (kid == null) {
				// Hit the bottom.
				textLogger = (TextLogger)logger;
				break;
			}
			// Keep going.
			logger = kid;
		}
		textLogger.addOutput(formatter);
	}

}
