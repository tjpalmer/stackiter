package stackiter.agents;

import static java.lang.Math.*;
import static stackiter.sim.Util.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import stackiter.sim.*;

/**
 * Build "Balance Scale Problem" instances (Siegler, 1976).
 */
public class BalanceScaleAgent extends BasicAgent {

	private static final double POST_EXTENT_Y = 5.0;

	private static final int WEIGHT_COUNT_MAX = 21;

	private static enum Mode {

		BUILD,

		DONE,

		SPILL,

		WAIT_FOR_DONE,

		WAIT_FOR_SPILL,

		WAIT_FOR_START,

	}

	private static class Stack {
		Block base;
		double height;
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
			// All set up all at once.
			getWorld().episodeStarted();
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
			if (waitCount < 500) {
				// We haven't waited too long, so we might keep going.
				// First see if the beam has tipped. If so, we're done. Early
				// exit shrinks log sizes and also avoids cases where some
				// blocks slide off then the balance shifts.
				// TODO Reconsider against homonyms?
				if (abs(beam.getAngle()) < 0.05) {
					// Beam isn't tipped, so check if weights are stable.
					for (Block weight: weights) {
						if (norm(weight.getLinearVelocity()) >= 1e-2) {
							// Nope, so we need to keep watching for a spill.
							allDone = false;
							break;
						}
					}
				}
			}
			if (allDone) {
				// We have a determined outcome. Wait a few, and be done.
				// TODO Is the waiting to make sure we get the new state logged?
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
			// TODO Combine wait for spill with wait for start?
			if (waitCount >= 20) {
				// Call that good enough.
				mode = Mode.SPILL;
				waitCount = 0;
			}
			break;
		case WAIT_FOR_START:
			// TODO Combine wait for spill with wait for start?
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

	private boolean overlap(Block a, Block b) {
		double aX = a.getPosition().getX();
		double aExtentX = a.getExtent().getX();
		double bX = b.getPosition().getX();
		double bExtentX = b.getExtent().getX();
		// Right edge past left and left past right.
		return aX + aExtentX >= bX - bExtentX && aX - aExtentX <= bX + bExtentX;
	}

	/**
	 * See if the center of `a` overlaps `b` horizontally.
	 */
	private boolean overlapCenterA(Block a, Block b) {
		double aX = a.getPosition().getX();
		double bX = b.getPosition().getX();
		double bExtentX = b.getExtent().getX();
		// Center between left and right.
		return aX >= bX - bExtentX && aX <= bX + bExtentX;
	}

	private void placeWeights() {
		weights.clear();
		int weightCount =
			// Expanded.
			//WEIGHT_COUNT_MAX + getRandom().nextInt(WEIGHT_COUNT_MAX / 2) + 1;
			// Standard.
			//getRandom().nextInt(WEIGHT_COUNT_MAX + 1);
			// Exact.
			WEIGHT_COUNT_MAX;
		List<Stack> stacks = new ArrayList<Stack>();
		for (int w = 0; w < weightCount; w++) {
			Block weight = new Block();
			weight.setColor(getWorld().getTray().randomColor());
			// All weights are same size, shape, and mass.
			weight.setExtent(2.0, 2.0);
			// Pick random x over beam, and place y above it.
			weight.setPosition(
				randomX(),
				beam.getExtent().getY() + beam.getPosition().getY() +
					weight.getExtent().getY()
			);
			// If x overlaps another stack, put it on top.
			boolean anyOverlap = false;
			Stack chosenStack = null;
			for (Stack stack: stacks) {
				if (overlap(weight, stack.base)) {
					anyOverlap = true;
					// Some noise to avoid exact alignment.
					do {
						weight.setPosition(
							stack.base.getPosition().getX() +
								0.2 * getRandom().nextGaussian(),
							weight.getPosition().getY()
						);
					} while (!overlapCenterA(weight, stack.base));
					// But stack past the top.
					weight.setPosition(
						weight.getPosition().getX(),
						stack.height + weight.getExtent().getY()
					);
					chosenStack = stack;
					stack.height += 2.0 * weight.getExtent().getY();
					// Might could still overlap another stack later, but don't
					// worry too much.
					break;
				}
			}
			if (chosenStack == null) {
				// It's a new stack.
				chosenStack = new Stack();
				chosenStack.base = weight;
				stacks.add(chosenStack);
			}
			// Update stack height for the new block.
			chosenStack.height =
				weight.getPosition().getY() + weight.getExtent().getY();
			// And track our weights.
			weights.add(weight);
		}
		// Shuffle then add weights to the world, so ids don't relate to
		// position at all. Probably doesn't matter, but eh.
		Collections.shuffle(weights);
		for (Block weight: weights) {
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
