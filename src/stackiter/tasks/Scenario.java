package stackiter.tasks;

import static stackiter.sim.Util.*;
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
	 * Adds nothing extra.
	 */
	class Empty implements Scenario {
		@Override
		public void buildWorld(World world) {
			// Just empty.
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

	void buildWorld(World world);

}
