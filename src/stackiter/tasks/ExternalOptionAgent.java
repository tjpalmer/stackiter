package stackiter.tasks;

import static java.lang.Double.*;
import static java.lang.Integer.*;
import static stackiter.sim.Util.*;

import java.io.*;
import java.util.*;

import stackiter.agents.*;
import stackiter.sim.*;

/**
 * Allow an external controller to control an agent through standard in and out.
 */
public class ExternalOptionAgent implements OptionAgent {

	/**
	 * Our options factory.
	 */
	private Options options;

	/**
	 * For reading lines from standard in.
	 */
	private BufferedReader reader;

	/**
	 * Track this for now just so we can request quitting.
	 *
	 * TODO Instead provide quit command through option???
	 */
	private World world;

	public ExternalOptionAgent(World world) {
		this.world = world;
		options = new Options(world.getTray().getRandom());
		reader = new BufferedReader(new InputStreamReader(System.in));
	}

	@Override
	public Option act(State state) {
		// Output the full current state of the blocks.
		// Start with blank line for visual parsing.
		System.out.println();
		// We purposely don't close the logger. It would close system out.
		@SuppressWarnings("resource")
		TextLogger logger = new TextLogger(new Formatter(System.out));
		logger.logSimTime(state.steps, state.simTime);
		for (Item item: state.items.values()) {
			logger.logItem(item);
		}
		logger.flush();
		// Another blank line after.
		System.out.println();

		// Read and parse command.
		Option option = null;
		try {
			// TODO Support state output to external controller?
			// TODO Change to socket or support console option?
			String command = reader.readLine();
			List<String> args = Arrays.asList(command.trim().split("\\s+"));
			// Now call the command the first token.
			command = args.isEmpty() ? "" : args.get(0);

			// TODO Map of handlers!
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

		// Good to go, and don't actually close the logger.
		return option;
	}

}
