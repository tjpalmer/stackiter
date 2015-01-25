package stackiter.agents;

import java.util.*;

import stackiter.sim.*;

/**
 * Deliver precisely the kinds of blocks needed for building an arch.
 */
public class ArchRefillAgent extends RefillAgent {

	public ArchRefillAgent() {
		super(3);
	}

	@Override
	public Block generateBlock() {
		Block block = new Block();
		Tray tray = getWorld().getTray();
		Random random = tray.getRandom();
		block.setColor(tray.randomColor());
		double maxExtent = tray.getMaxBlockExtent().getX();
		// Make them wide enough that pseudo-arches are still perhaps doable by
		// chance.
		// TODO Consider Gaussian noise on sizes.
		block.setExtent(maxExtent * 0.8, maxExtent * 0.4);
		// This will tend to make them all fall flat.
		// TODO Should I choose to let them sometimes fall upright?
		block.setRotation(2 * random.nextDouble() - 1);
		return block;
	}

}
