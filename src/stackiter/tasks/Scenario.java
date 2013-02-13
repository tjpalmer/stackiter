package stackiter.tasks;

import static stackiter.sim.Util.*;

import java.util.*;

import stackiter.agents.*;
import stackiter.sim.*;

/**
 * Sets up a world situation, including agents and block options.
 */
public interface Scenario {

	/**
	 * Just an alternating dropping agent for now.
	 *
	 * TODO Should the every-other clearing be done in the same agent?
	 */
	public class Alternate implements Scenario {
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
	class Babble implements Scenario {
		@Override
		public void buildWorld(World world) {
			world.addAgent(new ClearerAgent(30));
			world.addAgent(new DropperAgent());
		}
	}

	/**
	 * Constructs automated balance scale scenes.
	 */
	class BalanceScale implements Scenario {
		@Override
		public void buildWorld(World world) {
			world.addAgent(new BalanceScaleAgent());
			world.setTrayHeight(0);
		}
	}

	/**
	 * Provides a builder that likes to build towers.
	 */
	class Builder implements Scenario {
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
	class DropOn implements Scenario {
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
	class Empty implements Scenario {
		@Override
		public void buildWorld(World world) {
			// Just empty.
		}
	}

	/**
	 * Drop a bunch of blocks all over.
	 * TODO Parameterize quantity or other aspects?
	 */
	public class Predeployed implements Scenario {
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
	 * Constrains blocks to be small squares.
	 */
	class SmallSquares implements Scenario {
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
	public class WideTable implements Scenario {
		@Override
		public void buildWorld(World world) {
			world.getGround().setExtent(100, 1.5);
			world.setTrayHeight(0);
		}
	}

	void buildWorld(World world);

}
