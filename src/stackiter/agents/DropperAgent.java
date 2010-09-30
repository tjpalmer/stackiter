package stackiter.agents;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import stackiter.sim.*;

/**
 * Drops items randomly over the ground.
 */
public class DropperAgent extends BasicAgent {

	private enum Mode {

		/**
		 * No item is grasped.
		 */
		EMPTY,

		/**
		 * The item is grasped but not yet raised.
		 */
		GRASPED,

		/**
		 * The item is raised but not yet at the right drop point.
		 */
		RAISED,

	}

	/**
	 * Close is good enough here.
	 */
	private static final double EPSILON = 0.1;

	private Mode mode = Mode.EMPTY;

	private Random random = new Random();

	private int stableCount;

	private Item targetItem;

	/**
	 * Actually, so long as the tools really can go instantly, there's no need
	 * for this separate target. Well, if we also don't care about so much snap.
	 * But leave it here for now, anyway.
	 */
	private Point2D targetPosition = point();

	private Tool tool;

	@Override
	public void act() {
		Iterable<Item> items = getWorld().getItems();
		// Find our target.
		CURRENT: if (targetItem != null) {
			// Find the new state of the current target.
			for (Item item: items) {
				if (item.getSoul() == targetItem.getSoul()) {
					targetItem = item;
					break CURRENT;
				}
			}
			// The target is missing now.
			clearTarget();
			// Let the release take effect.
			return;
		}
		if (targetItem == null) {
			// Find a new something to grab.
			List<Item> options = new ArrayList<Item>();
			for (Item item: items) {
				if (!item.isAlive() && item instanceof Block) {
					options.add(item);
				}
			}
			if (!options.isEmpty()) {
				targetItem = options.get(random.nextInt(options.size()));
			}
		}
		switch (mode) {
			case EMPTY: {
				if (tool.getMode() == ToolMode.INACTIVE) {
					// We haven't actually grasped yet.
					if (targetItem != null) {
						// Find the max speed so we can know if we're stable.
						double maxSpeedSquared = 0;
						for (Item item: items) {
							double angularVelocity = item.getAngularVelocity();
							maxSpeedSquared = Math.max(angularVelocity * angularVelocity, maxSpeedSquared);
							maxSpeedSquared = Math.max(item.getLinearVelocity().distanceSq(0, 0), maxSpeedSquared);
						}
						if (maxSpeedSquared < 0.0001) {
							stableCount++;
						}
						// See if we've been stable for 0.1 seconds.
						if (stableCount >= 10) {
							tool.setPosition(targetItem.getPosition());
							tool.setMode(ToolMode.GRASP);
						}
					}
				} else {
					// We've already grabbed something, we think.
					// Raise (or lower) the block to the drop altitude.
					// If high enough, this won't usually get in the way of other blocks.
					targetPosition.setLocation(tool.getPosition().getX(), 25);
					tool.setPosition(targetPosition);
					mode = Mode.GRASPED;
				}
				break;
			}
			case GRASPED: {
				if (approx(targetItem.getPosition(), targetPosition, EPSILON)) {
					// Find a new random spot somewhere over the ground.
					Block ground = getWorld().getGround();
					Rectangle2D bounds = translated(ground.getBounds(), ground.getPosition());
					double targetX = random.nextDouble()*bounds.getWidth() + bounds.getMinX();
					targetPosition.setLocation(targetX, tool.getPosition().getY());
					tool.setPosition(targetPosition);
					mode = Mode.RAISED;
				} else {
					// Because of constraints, we need to keep resetting this until we get there.
					tool.setPosition(targetPosition);
				}
				break;
			}
			case RAISED: {
				if (approx(targetItem.getPosition(), targetPosition, EPSILON)) {
					clearTarget();
				} else {
					// Because of constraints, we need to keep resetting this until we get there.
					tool.setPosition(targetPosition);
				}
				break;
			}
		}
	}

	private void clearTarget() {
		targetItem = null;
		tool.setMode(ToolMode.INACTIVE);
		mode = Mode.EMPTY;
		stableCount = 0;
	}

	@Override
	protected void init() {
		tool = getWorld().addTool();
		// TODO Should we stylesheet agent colors instead?
		tool.setColor(Color.GREEN);
	}

}
