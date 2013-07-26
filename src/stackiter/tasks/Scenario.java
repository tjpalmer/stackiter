package stackiter.tasks;

import static stackiter.sim.Util.*;

import java.util.*;

import stackiter.agents.*;
import stackiter.sim.*;

/**
 * Sets up a world situation, including agents and block options.
 */
public class Scenario {

	/**
	 * Just an alternating dropping agent for now.
	 *
	 * TODO Should the every-other clearing be done in the same agent?
	 */
	public static class Alternate extends Scenario {
		@Override
		public void buildWorld(World world) {
			world.addAgent(new AlternateAgent());
			world.setTrayHeight(0);
		}
	}

	/**
	 * Adds a random dropper agent and one that clears the world every 30
	 * seconds.
	 */
	public static class Babble extends Scenario {
		@Override
		public void buildWorld(World world) {
			world.addAgent(new ClearerAgent(30));
			world.addAgent(new DropperAgent());
		}
	}

	/**
	 * Constructs automated balance scale scenes.
	 */
	public static class BalanceScale extends Scenario {
		@Override
		public void buildWorld(World world) {
			world.addAgent(new BalanceScaleAgent());
			world.setTrayHeight(0);
		}
	}

	/**
	 * Provides a builder that likes to build towers.
	 */
	public static class Builder extends Scenario {
		@Override
		public void buildWorld(World world) {
			//world.addAgent(new BuilderAgent());
			world.addAgent(new OptionAgent.Bridge(
				new BuilderOptionAgent(world.getTray().getRandom())
			));
		}
	}

	/**
	 * Constructs scale scenes for drop(X, Y).
	 */
	public static class DropOn extends Scenario {
		@Override
		public void buildWorld(World world) {
			world.addAgent(new DropOnAgent());
			world.getGround().setExtent(35, 1.5);
			world.setTrayHeight(0);
		}
	}

	/**
	 * Adds nothing extra.
	 */
	public static class Empty extends Scenario {
		@Override
		public void buildWorld(World world) {
			// Just empty.
		}
	}

	/**
	 * Adds an agent controlled by external input.
	 */
	public static class ExternalControl extends Scenario {
		@Override
		public void buildWorld(World world) {
			ExternalOptionAgent agent = new ExternalOptionAgent(world);
			world.addAgent(new OptionAgent.Bridge(agent));
		}
	}

	/**
	 * Drop a bunch of blocks all over.
	 * TODO Parameterize quantity or other aspects?
	 */
	public static class Predeployed extends Scenario {
		@Override
		public void buildWorld(World world) {
			int blockCount = 10;
			Random random = world.getTray().getRandom();
			// Max sure we can keep the blocks above the ground.
			// TODO Could also do this on individual block size.
			double maxRadius = norm(world.getTray().getMaxBlockExtent());
			for (int i = 0; i < blockCount; i++) {
				Block block = world.getTray().randomBlock();
				double x = randInRange(random, -30, 30);
				double y = randInRange(random, maxRadius, 40);
				block.setPosition(x, y);
				world.addBlock(block);
			}
		}
	}

	/**
	 * Always drop new blocks when the world is empty.
	 * TODO Parameterize quantity or other aspects?
	 */
	public static class Refill extends Scenario {
		@Override
		public void buildWorld(World world) {
			world.addAgent(new RefillAgent(5));
		}
	}

	/**
	 * Constrains blocks to be small squares.
	 */
	public static class SmallSquares extends Scenario {
		@Override
		public void buildWorld(World world) {
			Tray tray = world.getTray();
			tray.setMaxBlockExtent(point(1,1));
			tray.setMinBlockExtent(point(1,1));
		}
	}

	/**
	 * Wide table (ground) to present anything easily falling off.
	 */
	public static class WideTable extends Scenario {
		@Override
		public void buildWorld(World world) {
			world.getGround().setExtent(100, 1.5);
			world.setTrayHeight(0);
		}
	}

	/**
	 * Build the world using the scenarios given, providing the logger after
	 * setup.
	 */
	public static void handleWorldSetup(
		Iterable<Scenario> scenarios, World world, Logger logger
	) {
		for (Scenario scenario: scenarios) {
			scenario.buildWorld(world, logger);
		}
		world.setLogger(logger);
	}

	/**
	 * Override this if you don't care about the logger.
	 */
	public void buildWorld(World world) {}

	/**
	 * Override this if you want logger access.
	 * The logger gets added to world only after world setup, because there's
	 * some careful fine tuning of what gets logged when.
	 * However, some scenarios might want to affect logging.
	 *
	 * TODO Better would be to make the inital logging more robust, so the
	 * TODO world could already have a logger attached.
	 */
	public void buildWorld(World world, Logger logger) {
		buildWorld(world);
	}

}
