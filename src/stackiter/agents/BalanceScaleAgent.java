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

	private static final int WEIGHT_COUNT_MAX = 4;

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

	private void placeWeights() {
		weights.clear();
		int weightCount =
			// Expanded.
			WEIGHT_COUNT_MAX + getRandom().nextInt(WEIGHT_COUNT_MAX / 2) + 1;
			// Standard.
			//getRandom().nextInt(WEIGHT_COUNT_MAX + 1);
		for (int w = 0; w < weightCount; w++) {
			Block weight = new Block();
			weight.setColor(getWorld().getTray().randomColor());
			// All weights are same size, shape, and mass.
			weight.setExtent(2.0, 2.0);
			double extentX = weight.getExtent().getX();
			double extentY = weight.getExtent().getY();
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
					x + extentX >= otherX - otherExtentX &&
					x - extentX <= otherX + otherExtentX
				) {
					// Align to the bottom one.
					if (!xAligned) {
						double alignExtent = 0.95 * otherExtentX;
						do {
							// Some noise to avoid exact alignment.
							x = otherX + 0.2 * getRandom().nextGaussian();
						} while (
							// Retain the center above the bottom block.
							x < otherX - alignExtent || x > otherX + alignExtent
						);
						xAligned = true;
					}
					// But stack past the top.
					y = other.getPosition().getY() + other.getExtent().getY() +
						extentY;
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
