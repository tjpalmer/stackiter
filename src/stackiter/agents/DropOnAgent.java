package stackiter.agents;

import static stackiter.sim.Util.*;

import java.awt.geom.*;
import java.util.*;

import stackiter.sim.*;

/**
 * Specifically model a drop(X, Y) action where we care about whether the
 * dropped block (X) ends over the support block (Y).
 */
public class DropOnAgent extends BasicAgent {

	private static enum Mode {

		CONJURE_DROPPED,

		CONJURE_SUPPORT,

		DONE,

	}

	private static final double SUPPORT_LENGTH_MIN = 1.5;

	private static final double SUPPORT_LENGTH_RANGE = 4.0;

	private static final int MAX_DROPPEDS = 4;

	private static final int MIN_DROPPEDS = 1;

	private Block dropped;

	int droppedsToGo;

	private Mode mode = Mode.CONJURE_SUPPORT;

	private Block support;

	int waitCount;

	@Override
	public void act() {
		switch (mode) {
		case CONJURE_DROPPED:
			if (dropped == null || hasStoppedMoving(dropped)) {
				if (droppedsToGo == 0) {
					waitCount = 0;
					mode = Mode.DONE;
				} else {
					conjureDropped();
					droppedsToGo--;
					if (droppedsToGo == 0) {
						// That was it. We've started the episode.
						getWorld().episodeStarted();
					}
				}
			}
			break;
		case CONJURE_SUPPORT:
			conjureSupport();
			mode = Mode.CONJURE_DROPPED;
			break;
		case DONE:
			waitCount++;
			if (waitCount >= 5) {
				// Call that good enough.
				getWorld().clearBlocks();
				mode = Mode.CONJURE_SUPPORT;
			}
			break;
		}
	}

	private void conjureDropped() {
		// Place the goal at a Gaussian sampled point above the center of
		// the dropped block.
		double lengthMid = SUPPORT_LENGTH_MIN + 0.5 * SUPPORT_LENGTH_RANGE;
		double dropX =
			support.getPosition().getX() +
			lengthMid * getRandom().nextGaussian();
		// The y needs to be above the top of the dropped block, so to keep
		// it simple, use a uniform distribution, for scale uniformity with the
		// same bounds as the block length itself. Well, actually, add a bit so
		// piled up blocks don't interfere with initial placement.
		double supportMaxY = support.transformedShape().getBounds2D().getMaxY();
		double dropY =
				supportMaxY + randomSupportLength() + 2 * SUPPORT_LENGTH_RANGE;
		// TODO Random size?
		// Block.
		dropped = new Block();
		dropped.setColor(getWorld().getTray().randomColor());
		dropped.setExtent(point(1, 1));
		dropped.setAngle(0.5 * getRandom().nextGaussian());
		dropped.setPosition(dropX, dropY);
		// Add it.
		getWorld().addBlock(dropped);
	}

	private void conjureSupport() {
		double droppedLength = randomSupportLength();
		conjureSupport(point(droppedLength, 1));
		// Figure out how many extras we want.
		droppedsToGo =
				getRandom().nextInt(MAX_DROPPEDS - MIN_DROPPEDS + 1) +
				MIN_DROPPEDS;
		mode = Mode.CONJURE_DROPPED;
	}

	private void conjureSupport(Point2D extent) {
		// Use the tray's random, in case that's been configured.
		// TODO Better random management.
		support = new Block();
		support.setColor(getWorld().getTray().randomColor());
		support.setExtent(extent);
		// Angle.
		boolean upright = getRandom().nextDouble() < 0.5;
		support.setAngle(upright ? 0 : 0.5);
		// Position with base at ground.
		support.setPosition(randomX(), upright ? extent.getY() : extent.getX());
		// Add it.
		getWorld().addBlock(support);
	}

	private Random getRandom() {
		return getWorld().getTray().getRandom();
	}

	private boolean hasStoppedMoving(Item item) {
		// Gone means not moving.
		if (!item.isAlive()) return true;
		double speed = norm(item.getLinearVelocity());
		return speed < 1e-3;
	}

	private double randomSupportLength() {
		return
			SUPPORT_LENGTH_MIN +
			SUPPORT_LENGTH_RANGE * getRandom().nextDouble();
	}

	private double randomX() {
		// Keep center of mass over ground.
		return 0.95 * (2.0 * getRandom().nextDouble() - 1.0) *
			getWorld().getGround().getExtent().getX();
	}

}
