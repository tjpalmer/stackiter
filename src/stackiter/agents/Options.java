package stackiter.agents;

import static stackiter.sim.Util.*;

import java.awt.geom.*;

import stackiter.agents.OptionAgent.Action;
import stackiter.agents.OptionAgent.Option;
import stackiter.agents.OptionAgent.State;
import stackiter.sim.*;

/**
 * A set of perhaps convenient options.
 */
public class Options {

	public static final double EPSILON = 1e-2;

	/**
	 * Carry(x) carries an item to a goal position and waits for it to slow down
	 * enough.
	 */
	public static class Carry implements Option {

		public Point2D goal;

		public Carry(Point2D goal) {
			this.goal = goal;
		}

		@Override
		public Action act(State state) {
			// Default to no change.
			Action action = new Action(state.tool);
			// First verify status.
			if (!done(state)) {
				// Stay grasping, and move toward specified goal.
				action.tool.active = true;
				action.tool.position.setLocation(goal);
			}
			return action;
		}

		@Override
		public boolean done(State state) {
			Item graspedItem = state.graspedItem;
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

	/**
	 * Drop releases the grasped item and waits for the item to hit a surface
	 * below (identified for now by a reduction in speed).
	 */
	public static class Drop implements Option {

		public Item item;

		public double lastSpeed;

		public Drop(State state) {
			// TODO Note that for easier relational learning, the thing to be
			// TODO dropped should be an argument.
			// TODO Some DropOn action should also have the support as an arg.
			this.item = state.graspedItem;
			// Explicitly set to 0 to make that clear.
			lastSpeed = 0.0;
		}

		@Override
		public Action act(State state) {
			// Just let go and/or stay letting go.
			// TODO Check first if done?
			Action action = new Action(state.tool);
			action.tool.active = false;
			return action;
		}

		@Override
		public boolean done(State state) {
			if (item == null) {
				// Never had anything. We're done even if not ideally.
				return true;
			}
			if (state.tool.active) {
				// Still holding it.
				// TODO Actually check that we have a grasped item with the
				// TODO right soul?
				return false;
			}
			Item liveItem = state.items.get(item.getSoul());
			if (liveItem == null) {
				// It must have died.
				return true;
			}
			// We've let go. See if it's slowing down.
			double speed = norm(liveItem.getLinearVelocity());
			boolean slowing = speed < lastSpeed;
			lastSpeed = speed;
			return slowing;
		}

	}

	/**
	 * Grasp(x) attempts to grasp item x at its center, first releasing any
	 * currently held item.
	 */
	public static class Grasp implements Option {

		public Soul item;

		/**
		 * Nothing null.
		 */
		public Grasp(Soul item) {
			this.item = item;
		}

		@Override
		public Action act(State state) {
			// Default to no change.
			Action action = new Action(state.tool);
			// If not done, do something.
			if (!done(state)) {
				// Go to the right place.
				Item liveItem = state.items.get(item);
				// And grasp or ungrasp as appropriate.
				if (liveItem != null) {
					action.tool.position.setLocation(liveItem.getPosition());
					// If we aren't grasping, we need to grasp.
					// If we are grasping (but not done), we must be grasping
					// the wrong thing.
					// So just negate the active mode here.
					// World update order is: (1) release, (2) move, (3) grasp.
					// Can't do all three at once, however, since a change in
					// tool mode (active) needs to be registered.
					action.tool.active = !state.tool.active;
				}
			}
			return action;
		}

		@Override
		public boolean done(State state) {
			Item graspedItem = state.graspedItem;
			return graspedItem != null && graspedItem.getSoul() == item;
		}

	}

	private Options() {}

}
