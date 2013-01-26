package stackiter.agents;

import static stackiter.sim.ToolMode.*;

import java.awt.geom.*;

import stackiter.sim.*;

import static stackiter.sim.Util.*;

/**
 * Set of temporally extended actions for higher level control.
 */
public abstract class ActionAgents {

	public static final double EPSILON = 1e-2;

	public static abstract class Action extends BasicAgent {
		public abstract boolean done();
	}

	public static class Carry extends Action {

		public Point2D goal;

		public Tool tool;

		public Carry(World world, Tool tool, Point2D goal) {
			this.goal = goal;
			this.tool = tool;
			setWorld(world);
		}

		@Override
		public void act() {
			// First verify status.
			if (done()) return;
			// Action is easy here.
			tool.setPosition(goal);
		}

		@Override
		public boolean done() {
			Item graspedItem = getWorld().getGraspedItem(tool);
			if (graspedItem == null) {
				// Failure case, but that's all we can do.
				// TODO Some way of reporting failure?
				return true;
			}
			// Are we near?
			if (approx(graspedItem.getPosition(), goal, EPSILON)) {
				// Are we stopped? TODO Do we want to enforce this?
				if (approx(
					norm(graspedItem.getLinearVelocity()), 0.0, EPSILON
				)) {
					return true;
				}
			}
			// Not there yet.
			return false;
		}

	}

	public static class Drop extends Action {

		public Item item;

		public double lastSpeed;

		public Tool tool;

		public Drop(World world, Tool tool) {
			// TODO Note that for easier relational learning, the thing to be
			// TODO dropped should be an argument.
			// TODO Some DropOn action should also have the support as an arg.
			this.item = world.getGraspedItem(tool);
			this.tool = tool;
			// Explicitly set to 0 to make that clear.
			lastSpeed = 0.0;
			setWorld(world);
		}

		@Override
		public void act() {
			// First verify status.
			if (done()) return;
			// Action is easy here.
			tool.setMode(INACTIVE);
		}

		@Override
		public boolean done() {
			if (item == null) {
				// Never had anything. We're done even if not ideally.
				return true;
			}
			if (tool.getMode() == GRASP) {
				// Still holding it.
				// TODO Actually check that we have a grasped item with the
				// TODO right soul?
				return false;
			}
			if (!getWorld().getItemSouls().contains(item.getSoul())) {
				// It must have died.
				return true;
			}
			// We've let go. See if it's slowing down.
			// TODO Get item by soul instead of assuming unchanged.
			double speed = norm(item.getLinearVelocity());
			boolean slowing = speed < lastSpeed;
			lastSpeed = speed;
			return slowing;
		}

	}

	public static class Grasp extends Action {

		public Item item;

		public Tool tool;

		/**
		 * Nothing null.
		 */
		public Grasp(World world, Tool tool, Item item) {
			this.item = item;
			this.tool = tool;
			// Do this last, to prepare for init, if wanted.
			setWorld(world);
		}

		@Override
		public void act() {
			// First verify status.
			if (done()) return;

			// Go to the right place.
			// TODO Use the soul instead to look through current world items
			// TODO for current position.
			tool.setPosition(item.getPosition());

			// And grasp or ungrasp as appropriate.
			if (tool.getMode() != INACTIVE) {
				// I think the ungrasp happens before the move.
				// TODO Double-check this.
				tool.setMode(INACTIVE);
			} else {
				tool.setMode(GRASP);
			}
		}

		@Override
		public boolean done() {
			// TODO Work on soul level?
			Item graspedItem = getWorld().getGraspedItem(tool);
			return
				graspedItem != null && graspedItem.getSoul() == item.getSoul();
		}

	}

	private ActionAgents() {}

}
