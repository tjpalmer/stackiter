package stackiter.agents;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import stackiter.sim.*;

/**
 * Conjures up blocks to drop in alternating order: first a long one, either
 * upright or flat, then a square over a random ground point.
 */
public class AlternateAgent extends BasicAgent {

	private static enum Mode {

		/**
		 * Finish clearing the world.
		 */
		CLEAR,

		/**
		 * Conjure blocks.
		 */
		CONJURE,

		/**
		 * Drop the square block.
		 */
		DROP,

		/**
		 * Wait for before starting again.
		 */
		WAIT,

	}

	private Block block;

	private Block dropped;

	private double goalX;

	private double goalY;

	private Mode mode;

	private Tool tool;

	private int waitCount;

	@Override
	public void act() {
		// I only use a tool here because my later parsing code might expect it.
		if (mode == Mode.CONJURE) {
			double lengthMin = 1.5;
			double lengthRange = 4.0;
			double droppedLength =
				lengthMin + lengthRange * getRandom().nextDouble();
			conjureDropped(point(droppedLength, 1));
			// Place the goal at a Gaussian sampled point above the center of
			// the dropped block.
			double lengthMid = lengthMin + 0.5 * lengthRange;
			goalX =
				dropped.getPosition().getX() +
				lengthMid * getRandom().nextGaussian();
			// The y needs to be above the top of the dropped block, so to keep
			// it simple, use a uniform distribution, for kicks with the same
			// bounds as
			double droppedMaxY =
				dropped.transformedShape().getBounds2D().getMaxY();
			goalY =
				droppedMaxY + lengthMin +
				lengthRange * getRandom().nextDouble();
		}
		tool.setPosition(point(goalX, goalY));
		switch (mode) {
			case CLEAR:
				tool.setMode(ToolMode.INACTIVE);
				mode = Mode.CONJURE;
				break;
			case CONJURE:
				conjureGrasped(point(1, 1));
				mode = Mode.DROP;
				break;
			case DROP:
				tool.setMode(ToolMode.INACTIVE);
				if (
					block.getPosition().getY() + 0.1 * block.getExtent().getY()
					< tool.getPosition().getY()
				) {
					// It is below the tool. Let's see if it has stopped.
					if (norm(block.getLinearVelocity()) < 1e-2) {
						// It has stopped. Wait a few.
						waitCount = 0;
						mode = Mode.WAIT;
					}
				}
				break;
			case WAIT:
				waitCount++;
				if (waitCount >= 5) {
					// Done waiting. Clear the world.
					block = null;
					tool.setPosition(getWorld().getClearer().getPosition());
					tool.setMode(ToolMode.GRASP);
					mode = Mode.CLEAR;
				}
				break;
			default:
				throw new RuntimeException("Unsupported mode: " + mode);
		}
	}

	private void conjureDropped(Point2D extent) {
		// Use the tray's random, in case that's been configured.
		// TODO Better random management.
		dropped = new Block();
		dropped.setColor(getWorld().getTray().randomColor());
		dropped.setExtent(extent);
		// Angle.
		boolean upright = getRandom().nextDouble() < 0.5;
		dropped.setAngle(upright ? 0 : 0.5);
		// Position with base at ground.
		dropped.setPosition(randomX(), upright ? extent.getY() : extent.getX());
		// Add it.
		getWorld().addBlock(dropped);
	}

	private void conjureGrasped(Point2D extent) {
		// Block.
		block = new Block();
		block.setColor(getWorld().getTray().randomColor());
		block.setExtent(extent);
		// Position at tool.
		block.setPosition(tool.getPosition());
		// Add and grasp it.
		getWorld().addBlock(block);
		tool.setMode(ToolMode.GRASP);
	}

	private Random getRandom() {
		return getWorld().getTray().getRandom();
	}

	@Override
	protected void init() {
		// Tool.
		tool = getWorld().addTool();
		tool.setColor(Color.GREEN);

		// Mode.
		mode = Mode.CONJURE;
	}

	private double randomX() {
		// Keep center of mass over ground.
		return 0.95 * (2.0 * getRandom().nextDouble() - 1.0) *
			getWorld().getGround().getExtent().getX();
	}

	private double randomY() {
		// Keep center of mass over ground.
		return 12.5 + 10 * getRandom().nextDouble();
	}

}
