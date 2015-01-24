package stackiter.agents;

import static stackiter.sim.Util.*;

import java.util.*;

import stackiter.sim.*;

/**
 * Whenever the world is empty, drops in new blocks.
 */
public class RefillAgent extends BasicAgent {

	private int count;

	public RefillAgent(int count) {
		this.count = count;
	}

	@Override
	public void act() {
		if (!getWorld().getBlocks().iterator().hasNext()) {
			// Empty world. Fill 'er up.
			fill();
		}
	}

	/**
	 * Fills in new blocks.
	 * Subclasses might choose to customize logic.
	 */
	public void fill() {
		World world = getWorld();
		Random random = world.getTray().getRandom();
		// Max sure we can keep the blocks above the ground.
		// TODO Could also do this on individual block size.
		double maxRadius = norm(world.getTray().getMaxBlockExtent());
		for (int i = 0; i < count; i++) {
			Block block = generateBlock();
			double x = randInRange(random, -30, 30);
			double y = randInRange(random, maxRadius, 40);
			block.setPosition(x, y);
			world.addBlock(block);
		}
	}

	public Block generateBlock() {
		return getWorld().getTray().randomBlock();
	}

}
