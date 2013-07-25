package stackiter.tasks;

import static java.lang.Double.*;
import static java.lang.Integer.*;
import static stackiter.sim.Util.*;

import java.util.*;

import stackiter.agents.*;
import stackiter.sim.*;

/**
 * Allow an external controller (such as across a socket) to control an agent.
 */
public class ExternalOptionAgent implements OptionAgent {

	private Formatter formatter;

	private TextLogger logger;

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

		// Parse command.
		Option option = null;
		// TODO Map of handlers!
		try {
			if (command.equals("carry")) {
				if (args.size() == 4) {
					int id = parseInt(args.get(1));
					double x = parseDouble(args.get(2));
					double y = parseDouble(args.get(3));
					Soul item = logger.getSoul(id);
					option = options.carry(item, point(x, y));
				} else {
					System.out.println("Usage: carry id x y");
				}
			} else if (command.equals("clear")) {
				option = options.clear();
			} else if (command.equals("drop")) {
				if (args.size() == 2) {
					int id = parseInt(args.get(1));
					Soul item = logger.getSoul(id);
					if (
						state.graspedItem != null &&
						state.graspedItem.getSoul() == item
					) {
						// The id matches. Drop it.
						option = options.drop(state);
					} else {
						System.out.printf("Item %d not grasped.\n", id);
					}
				} else {
					System.out.println("Usage: drop id");
				}
			} else if (command.equals("quit")) {
				world.requestQuit();
			}
		} catch (Exception e) {
			// Don't crash the system on this.
			e.printStackTrace();
		}
		// Default to delay.
		if (option == null) {
			option = options.delay();
		}

		// Good to go.
		return option;
	}

	public void listen(Logger logger) {
		// Get the deepest logger, presuming it's a TextLogger.
		while (true) {
			Logger kid = logger.getKid();
			if (kid == null) {
				// Hit the bottom.
				this.logger = (TextLogger)logger;
				break;
			}
			// Keep going.
			logger = kid;
		}
		this.logger.addOutput(formatter);
	}

}
