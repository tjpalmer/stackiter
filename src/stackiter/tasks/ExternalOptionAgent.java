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

	private boolean first = true;

	private Formatter formatter;

	private Logger logger;

	/**
	 * Our options factory.
	 */
	private Options options;

	/**
	 * For reading lines from standard in.
	 */
	private BufferedReader reader;

	private TextLogger textLogger;

	private World world;

	public ExternalOptionAgent(World world) {
		this.world = world;
		formatter = new Formatter(System.out);
		options = new Options(world.getTray().getRandom());
		reader = new BufferedReader(new InputStreamReader(System.in));
	}

	@Override
	public Option act(State state) {
		// Push out state before asking for the next command.
		logger.push();
		// Presumably the logger will flush things anyway, but just for good
		// measure ...
		formatter.flush();

		// Read and parse command.
		Option option = null;
		try {
			String command;
			if (first) {
				// Just delay the first time, so we get the world set up.
				command = "";
				first = false;
			} else {
				formatter.format("Ready:\n");
				command = reader.readLine();
				if (command == null) {
					// End of stream. They're done.
					//command = "quit";
				}
			}
			List<String> args = Arrays.asList(command.trim().split("\\s+"));
			// Now call the command the first token.
			command = args.isEmpty() ? "" : args.get(0);

			// TODO Map of handlers!
			if (command.equals("carry")) {
				if (args.size() == 4) {
					int id = parseInt(args.get(1));
					double x = parseDouble(args.get(2));
					double y = parseDouble(args.get(3));
					Soul item = textLogger.getSoul(id);
					option = options.carry(item, point(x, y));
				} else {
					System.out.println("Usage: carry id x y");
				}
			} else if (command.equals("clear")) {
				option = options.clear();
			} else if (command.equals("done")) {
				option = options.done();
			} else if (command.equals("drop")) {
				if (args.size() == 2) {
					int id = parseInt(args.get(1));
					Soul item = textLogger.getSoul(id);
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
			} else if (command.equals("isolate")) {
				if (args.size() == 2) {
					Soul item = textLogger.getSoul(parseInt(args.get(1)));
					option = options.isolate(item);
				} else {
					System.out.println("Usage: rotate id");
				}
			} else if (command.equals("lift")) {
				if (args.size() == 2) {
					int id = parseInt(args.get(1));
					Soul item = textLogger.getSoul(id);
					option = options.lift(item);
				} else {
					System.out.println("Usage: lift id");
				}
			} else if (command.equals("posit")) {
				if (args.size() == 3) {
					Soul item = textLogger.getSoul(parseInt(args.get(1)));
					double x = parseDouble(args.get(2));
					option = options.posit(item, x);
				} else {
					System.out.println("Usage: put id targetId");
				}
			} else if (command.equals("put")) {
				if (args.size() == 3) {
					Soul item = textLogger.getSoul(parseInt(args.get(1)));
					Soul target = textLogger.getSoul(parseInt(args.get(2)));
					option = options.put(item, target);
				} else {
					System.out.println("Usage: put id targetId");
				}
			} else if (command.equals("rotate")) {
				if (args.size() == 2) {
					Soul item = textLogger.getSoul(parseInt(args.get(1)));
					option = options.rotate(item);
				} else {
					System.out.println("Usage: rotate id");
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
		//System.out.println(option.meta().args);
		return option;
	}

	public void listen(Logger logger) {
		this.logger = logger;
		// Get the deepest logger, presuming it's a TextLogger.
		while (true) {
			Logger kid = logger.getKid();
			if (kid == null) {
				// Hit the bottom.
				this.textLogger = (TextLogger)logger;
				break;
			}
			// Keep going.
			logger = kid;
		}
		this.textLogger.addOutput(formatter);
	}

}
