package stackiter.agents;

import static stackiter.sim.Util.norm;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import stackiter.sim.BasicAgent;
import stackiter.sim.Block;

/**
 * Build "Balance Scale Problem" instances. Reference Siegler.
 */
public class BalanceScaleAgent extends BasicAgent {

	private static final double POST_EXTENT_Y = 5.0;

	private static final int WEIGHT_COUNT = 5;

	private static enum Mode {

		BUILD,

		DONE,

		SPILL,

		WAIT_FOR_DONE,

		WAIT_FOR_SPILL,

		WAIT_FOR_START,

	}

	private Block beam;

	private Mode mode = Mode.BUILD;

	private List<Block> weights = new ArrayList<Block>();

	int waitCount;

	@Override
	public void act() {
		waitCount++;
		switch (mode) {
		case BUILD:
			buildBalanceBeam();
			placeWeights();
			mode = Mode.WAIT_FOR_START;
			waitCount = 0;
			break;
		case DONE:
			// Just a single pause.
			mode = Mode.BUILD;
			break;
		case SPILL:
			boolean allDone = true;
			// Put in a maximum wait time, just in case, but make it big.
			if (waitCount < 5000) {
				// We haven't waited too long, so check the weights.
				// TODO Reconsider against homonyms?
				for (Block weight: weights) {
					if (weight.getPosition().getY() < POST_EXTENT_Y * 2.0) {
						// Who cares if it's moving. It's below the beam.
						continue;
					}
					if (norm(weight.getLinearVelocity()) >= 1e-2) {
						allDone = false;
						break;
					}
				}
			}
			if (allDone) {
				// All blocks stopped. Wait a few.
				mode = Mode.WAIT_FOR_DONE;
				waitCount = 0;
			}
			break;
		case WAIT_FOR_DONE:
			if (waitCount >= 5) {
				// Call that good enough.
				getWorld().clearBlocks();
				mode = Mode.BUILD;
			}
			break;
		case WAIT_FOR_SPILL:
			if (waitCount >= 20) {
				// Call that good enough.
				mode = Mode.SPILL;
				waitCount = 0;
			}
			break;
		case WAIT_FOR_START:
			if (waitCount >= 20) {
				// Call that good enough.
				mode = Mode.WAIT_FOR_SPILL;
				waitCount = 0;
			}
			break;
		}
	}

	private void buildBalanceBeam() {
		// Post.
		Block post = buildPost(0.0);
		// Beam.
		beam = new Block();
		beam.setColor(Color.GRAY);
		beam.setExtent(10.0, 0.25);
		beam.setPosition(
			0.0, beam.getExtent().getY() + 2.0 * post.getExtent().getY()
		);
		getWorld().addBlock(beam);
	}

	private Block buildPost(double x) {
		Block post = new Block();
		post.setColor(Color.DARK_GRAY);
		post.setExtent(1.0, POST_EXTENT_Y);
		post.setPosition(x, post.getExtent().getY());
		getWorld().addBlock(post);
		return post;
	}

	private void placeWeights() {
		weights.clear();
		int weightCount = getRandom().nextInt(WEIGHT_COUNT);
		for (int w = 0; w < weightCount; w++) {
			Block weight = new Block();
			weight.setColor(getWorld().getTray().randomColor());
			// All weights are same size, shape, and mass.
			weight.setExtent(2.0, 2.0);
			// Pick random x over beam, and place y above it.
			double x = randomX();
			double y = beam.getExtent().getY() +
				beam.getPosition().getY() + weight.getExtent().getY();
			// If x overlaps another weight, put it exactly over it.
			boolean xAligned = false;
			for (Block other: weights) {
				double otherX = other.getPosition().getX();
				double otherExtentX = other.getExtent().getX();
				if (
					// Right edge past left and left past right.
					x + weight.getExtent().getX() >= otherX - otherExtentX &&
					x - weight.getExtent().getX() <= otherX + otherExtentX
				) {
					// Some noise to avoid exact alignment.
					if (!xAligned) {
						// Align to the bottom one.
						x = otherX + 0.2 * getRandom().nextGaussian();
						xAligned = true;
					}
					// But stack past the top.
					y += 2.0 * other.getExtent().getY();
				}
			}
			weight.setPosition(x, y);
			weights.add(weight);
			getWorld().addBlock(weight);
		}
	}

	private Random getRandom() {
		return getWorld().getTray().getRandom();
	}

	private double randomX() {
		// Keep center of mass over beam.
		return 0.95 * (2.0 * getRandom().nextDouble() - 1.0) *
			beam.getExtent().getX();
	}

}
