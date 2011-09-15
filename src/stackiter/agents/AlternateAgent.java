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

	private double goalX;

	private Mode mode;

	private Tool tool;

	private int waitCount;

	@Override
	public void act() {
		// I only use a tool here because my later parsing code might expect it.
		if (mode == Mode.CONJURE) {
			goalX = randomX();
		}
		tool.setPosition(point(goalX, 15));
		switch (mode) {
			case CLEAR:
				tool.setMode(ToolMode.INACTIVE);
				mode = Mode.CONJURE;
				break;
			case CONJURE:
				conjureDropped(point(5, 1));
				conjureGrasped(point(1, 1));
				mode = Mode.DROP;
				break;
			case DROP:
				tool.setMode(ToolMode.INACTIVE);
				if (
					block.getPosition().getY() + block.getExtent().getY() <
					tool.getPosition().getY()
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
		Random random = getWorld().getTray().getRandom();
		// Block.
		Block block = new Block();
		block.setColor(getWorld().getTray().randomColor());
		block.setExtent(extent);
		// Angle.
		boolean upright = random.nextDouble() < 0.5;
		block.setAngle(upright ? 0 : 0.5);
		// Position with base at ground.
		block.setPosition(randomX(), upright ? extent.getY() : extent.getX());
		// Add it.
		getWorld().addBlock(block);
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
		Random random = getWorld().getTray().getRandom();
		return 0.95 * (2.0 * random.nextDouble() - 1.0) *
			getWorld().getGround().getExtent().getX();
	}

}
