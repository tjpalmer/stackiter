package stackiter.agents;

import static stackiter.sim.Util.*;

import java.awt.geom.*;
import java.util.*;

import stackiter.agents.OptionAgent.Action;
import stackiter.agents.OptionAgent.Option;
import stackiter.agents.OptionAgent.State;
import stackiter.sim.*;

/**
 * A set of perhaps convenient options.
 * An Options instance works as a factory for the various option types.
 * By default, all options have a 500 step (5 sim second) timeout.
 */
public class Options {

	public static final double EPSILON = 1e-2;

	/**
	 * Used for noisy actions.
	 * For now, the issue is that the simulator takes commands exactly, and we
	 * observe exactly.
	 * As a human, exact observation isn't so easy, and we can't really move
	 * the mouse to any old exact point, but exactly modeling human limits here
	 * is hard.
	 * Therefore, let the options choose slightly wrong things.
	 * TODO Any noisiness at sim layer instead?
	 * TODO Use angle, distance commands from agents?
	 */
	private Random random;

	/**
	 * Carry(x) carries an item to a goal position and waits for it to slow down
	 * enough.
	 */
	private static class Carry implements Option {

		/**
		 * Sanity limit.
		 * <p>
		 * TODO Tie to actual world limits?
		 */
		private static final int MAX_HEIGHT = 200;

		public Point2D goal;

		/**
		 * Used for noisy actions.
		 */
		private Random random;

		public Carry(Point2D goal, Random random) {
			this.goal = goal;
			this.random = random;
		}

		@Override
		public Action act(State state) {
			// Default to no change.
			Action action = new Action(state.tool);
			// First verify status.
			if (!done(state)) {
				// Stay grasping, and move toward specified goal.
				action.tool.active = true;
				// We want the item at the destination, not the tool.
				Point2D graspOffset = subtracted(
					state.tool.position, state.graspedItem.getPosition()
				);
				Point2D toolGoal = added(goal, graspOffset);
				//System.out.println("Offset: " + graspOffset + ", goal: " + goal + ", tool goal: " + toolGoal);
				action.tool.position.setLocation(toolGoal);
			}
			return action;
		}

		@Override
		public boolean done(State state) {
			// Check sanity.
			if (goal.getY() > MAX_HEIGHT) {
				// Not sane. Live items get stuck outside bounds.
				return true;
			}
			// Check our cargo.
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

		@Override
		public String toString() {
			return "Carry(" + goal + ")";
		}

	}

	/**
	 * Drop releases the grasped item and waits for the item to hit a surface
	 * below (identified for now by a reduction in speed).
	 */
	private static class Drop implements Option {

		public Item item;

		public Drop(State state) {
			// TODO Note that for easier relational learning, the thing to be
			// TODO dropped should be an argument.
			// TODO Some DropOn action should also have the support as an arg.
			this.item = state.graspedItem;
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
			// Wait for everything to stop moving.
			for (Item item: state.items.values()) {
				if (norm(item.getLinearVelocity()) >= EPSILON) {
					return false;
				}
				// TODO Different epsilon for angular?
				if (Math.abs(item.getAngularVelocity()) >= EPSILON) {
					return false;
				}
			}
			// Everything's at rest (enough).
			return true;
		}

		@Override
		public String toString() {
			return "Drop";
		}

	}

	/**
	 * Grasp(x) attempts to grasp item x at its center, first releasing any
	 * currently held item.
	 */
	private static class Grasp implements Option {

		private Soul item;

		/**
		 * Used for noisy actions.
		 */
		private Random random;

		/**
		 * Nothing null.
		 */
		public Grasp(Soul item, Random random) {
			this.item = item;
			this.random = random;
		}

		@Override
		public Action act(State state) {
			// Default to no change.
			Action action = new Action(state.tool);
			// If not done, do something.
			if (!done(state)) {
				// Go to the right place.
				Item liveItem = state.items.get(item);
				if (liveItem != null) {
					Point2D graspPoint;
					double deviation = 0.5;
					while (true) {
						graspPoint = added(
							liveItem.getPosition(),
							point(
								deviation * random.nextGaussian(),
								deviation * random.nextGaussian()
							)
						);
						if (liveItem.contains(graspPoint)) {
							break;
						}
					}
					action.tool.position.setLocation(graspPoint);
					// And grasp or ungrasp as appropriate.
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

		@Override
		public String toString() {
			return "Grasp(" + item + ")";
		}

	}

	/**
	 * Consistently though hackishly avoid infinite loops.
	 */
	private static class TimeoutOption implements Option {

		private static final int MAX_WAIT = 500;

		private Option option;

		private int stepCount;

		public TimeoutOption(Option option) {
			this.option = option;
		}

		@Override
		public Action act(State state) {
			stepCount++;
			return option.act(state);
		}

		@Override
		public boolean done(State state) {
			if (stepCount > MAX_WAIT) {
				return true;
			}
			return option.done(state);
		}

		@Override
		public String toString() {
			return "Timeout(" + option + ")";
		}

	}

	public Options(Random random) {
		this.random = random;
	}

	public Option carry(Point2D goal) {
		return prepare(new Carry(goal, random));
	}

	public Option drop(State state) {
		return prepare(new Drop(state));
	}

	public Option grasp(Soul item) {
		return prepare(new Grasp(item, random));
	}

	/**
	 * Wraps and/or otherwise sets up the given option.
	 */
	public Option prepare(Option option) {
		return new TimeoutOption(option);
	}

}