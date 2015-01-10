package stackiter.agents;

import static stackiter.sim.Util.*;

import java.awt.geom.*;
import java.util.*;

import stackiter.agents.OptionAgent.Action;
import stackiter.agents.OptionAgent.Option;
import stackiter.agents.OptionAgent.State;
import stackiter.sim.*;
import stackiter.sim.Meta.Provider;

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
	 *
	 * On the other hand, any interpretation from data will think that the agent
	 * meant to target to particular location.
	 * This will result in wrong modeling.
	 * Okay anyway?
	 * TODO Record option info for more correctness?
	 *
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
		 * TODO X limits?
		 */
		private static final int MAX_HEIGHT = 200;

		public Point2D goal;

		/**
		 * Tracked so far just for meta logging.
		 */
		public Soul item;

		/**
		 * Customize for meta.
		 */
		public String name;

		public Point2D originalGoal;

		public Carry(Soul item, Point2D goal, Random random) {
			if (item == null) throw new RuntimeException("Null item.");
			if (goal == null) throw new RuntimeException("Null goal.");
			name = "carry";
			// Deviation of 1 might do.
			Point2D offset =
				point(random.nextGaussian(), random.nextGaussian());
			// Record our messed-up goal so we know when we've reached it.
			this.goal = added(goal, offset);
			this.item = item;
			this.originalGoal = goal;
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
		public Meta meta() {
			// Report our intent, not the lame after-noise.
			return new Meta(name, item, originalGoal);
		}

		@Override
		public String toString() {
			return "Carry(" + goal + ")";
		}

	}

	/**
	 * Just clear out the world.
	 */
	public class Clear implements Option {

		private double checkTime;

		private boolean cleared;

		private boolean done;

		@Override
		public Action act(State state) {
			Action action = new Action(state.tool);
			if (!cleared) {
				action.clear = true;
				// Give a bit of a delay before deciding that all is clear.
				checkTime = state.simTime + 0.1;
				cleared = true;
			}
			return action;
		}

		@Override
		public boolean done(State state) {
			if (cleared && state.simTime >= checkTime) {
				// Something else might have added new blocks.
				// If so, let them calm down.
				done = atRest(state);
			}
			return done;
		}

		@Override
		public Meta meta() {
			return new Meta("clear");
		}

	}

	/**
	 * Allows sequenced options as also suggested by Precup et al. (1998).
	 */
	public class Composed implements Option {

		private int current;

		private Option[] options;

		private Provider metaProvider;

		public Composed(Meta.Provider metaProvider, Option... options) {
			this.options = options;
			this.metaProvider = metaProvider;
			// Start at the first option. Be explicit.
			current = 0;
		}

		@Override
		public Action act(State state) {
			Option option;
			while (true) {
				if (done(state)) {
					// Already got through everything.
					return null;
				}
				// Still have more options to look at.
				option = options[current];
				if (!option.done(state)) {
					// Current one has work to do.
					break;
				}
				// That one's done. Move on.
				current++;
			}
			// Found an unfinished option. Use it.
			Action action = option.act(state);
			return action;
		}

		@Override
		public boolean done(State state) {
			return current >= options.length;
		}

		@Override
		public Meta meta() {
			// TODO Generate default if provide is null.
			return metaProvider.meta();
		}

	}

	/**
	 * Without item information, this delays the goal specification until it
	 * starts acting.
	 */
	public abstract static class DeferredGoalCarry extends Carry {

		private boolean goalChosen;

		public DeferredGoalCarry(Soul item, Random random) {
			super(item, point(), random);
		}

		@Override
		public Action act(State state) {
			chooseGoalIfNeeded(state);
			return super.act(state);
		}

		@Override
		public boolean done(State state) {
			chooseGoalIfNeeded(state);
			return super.done(state);
		}

		protected abstract void chooseGoal(State state);

		private void chooseGoalIfNeeded(State state) {
			if (goalChosen) return;
			chooseGoal(state);
			goalChosen = true;
		}

	}

	/**
	 * Waits a random amount of time before being done.
	 */
	public class Delay implements Option {

		/**
		 * Most delays between 0.6 and 1.4 seconds.
		 */
		private static final double DELAY_DEVIATION = 0.2;

		/**
		 * Wait about one second on average.
		 *
		 * Temporarily super big causes other timeout at 5 seconds, and we
		 * usually prefer longer.
		 *
		 * TODO Choose something centering around 5 instead and remove the other
		 * TODO timeout?
		 */
		private static final double DELAY_MEAN = 100.0;

		/**
		 * Ensure at least half a second (which changes the average).
		 */
		private static final double DELAY_MIN = 0.5;

		/**
		 * The first time we act.
		 */
		private double beginTime;

		/**
		 * The time to wait for.
		 */
		private double delayDuration;

		public Delay(Random random) {
			beginTime = Double.POSITIVE_INFINITY;
			// Loop to force at least some wait.
			// This probably pushes the mean up some, but oh well.
			do {
				delayDuration =
					DELAY_DEVIATION * random.nextGaussian() + DELAY_MEAN;
			} while (delayDuration < DELAY_MIN);
		}

		@Override
		public Action act(State state) {
			if (Double.isInfinite(beginTime)) {
				// Track the start.
				beginTime = state.simTime;
			}
			// Nothing to do.
			return null;
		}

		@Override
		public boolean done(State state) {
			return state.simTime >= beginTime + delayDuration;
		}

		@Override
		public Meta meta() {
			return new Meta("delay");
		}

	}

	/**
	 * Drop releases the grasped item and waits for the item to hit a surface
	 * below (identified for now by a reduction in speed).
	 */
	private static class Drop implements Option {

		public Soul item;

		public Drop(State state) {
			// TODO Note that for easier relational learning, the thing to be
			// TODO dropped should be an argument.
			// TODO Some DropOn action should also have the support as an arg.
			Item item = state.graspedItem;
			this.item = item == null ? null : item.getSoul();
		}

		public Drop(Soul item) {
			this.item = item;
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
			return atRest(state);
		}

		@Override
		public Meta meta() {
			return new Meta("drop", item);
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
			if (item == null) {
				throw new RuntimeException("Null item.");
			}
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
		public Meta meta() {
			return new Meta("grasp", item);
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
	 * A constrained carry that just lifts vertically.
	 */
	public static class Lift extends DeferredGoalCarry {

		/**
		 * Amount to lift by.
		 */
		private double amount;

		public Lift(Soul item, Random random) {
			this(item, random, 30);
		}

		public Lift(Soul item, Random random, double amount) {
			super(item, random);
			name = "lift";
			this.amount = amount;
		}

		@Override
		protected void chooseGoal(State state) {
			// Still need to choose a lift goal.
			// For now, just go to a rather tall height.
			// TODO Base this on max height of items not resting on current
			// TODO item?
			Item item = state.items.get(this.item);
			Point2D position = added(item.getPosition(), point(0, amount));
			// Noise will already have been added to the existing "0" goal.
			goal = added(goal, position);
		}

		@Override
		public Meta meta() {
			return new Meta(name, item);
		}

	}

	/**
	 * Move an item horizontally to place it above another.
	 */
	public static class PlaceX extends DeferredGoalCarry {

		private Soul target;

		public PlaceX(Soul item, Soul target, Random random) {
			super(item, random);
			if (target == null) throw new RuntimeException("Null target.");
			this.target = target;
			// TODO Underscores, hyphens, or camels?
			name = "placeX";
		}

		@Override
		protected void chooseGoal(State state) {
			// TODO What if either is null?
			Item item = state.items.get(this.item);
			Item target = state.items.get(this.target);
			if (target == null) {
				// Just give up and target where we already are.
				// TODO What if item is null?
				// TODO Seems less likely for current cases.
				target = item;
			}
			goal = added(
				// Noise will already have been added to the existing "0" goal.
				goal,
				point(target.getPosition().getX(), item.getPosition().getY())
			);
		}

		@Override
		public Meta meta() {
			return new Meta(name, item, target);
		}

	}

	/**
	 * Attempts to rotate the item by grabbing a side and lifting.
	 */
	public static class RotateGrasp implements Option {

		private Soul item;

		private Random random;

		public RotateGrasp(Soul item, Random random) {
			if (item == null) {
				throw new RuntimeException("Null item.");
			}
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
		public Meta meta() {
			return new Meta("rotate", item);
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
	 *
	 * Note that Precup et al. (1998) assume outer options don't interrupt the
	 * options to which they delegate.
	 * I need this interruption support here, though, and there might be other
	 * cases it also comes in handy.
	 * TODO Are there theoretical consequences?
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
		public Meta meta() {
			// Annotation not worth it at present, since I only use timeouts.
			//Meta meta = option.meta();
			//meta.name += "-timeout";
			// Just use underlying meta.
			return option.meta();
		}

		@Override
		public String toString() {
			return "Timeout(" + option + ")";
		}

	}

	private static boolean atRest(State state) {
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

	public Options(Random random) {
		this.random = random;
	}

	public Option carry(Soul item, Point2D goal) {
		// To simplify sequencing, make carry also grasp.
		Grasp grasp = new Grasp(item, random);
		Carry carry = new Carry(item, goal, random);
		// The first carry is the meta-provider.
		// That is, we're presuming this version just looks like a carry, even
		// though it is more than a Carry.
		Composed composed = new Composed(carry, grasp, carry);
		return prepare(composed);
	}

	public Option clear() {
		// Timeout's not needed here, but eh.
		return prepare(new Clear());
	}

	public Option delay() {
		return prepare(new Delay(random));
	}

	public Option drop(State state) {
		return prepare(new Drop(state));
	}

	public Option grasp(Soul item) {
		return prepare(new Grasp(item, random));
	}

	public Option lift(Soul item) {
		// To simplify sequencing, make lift also grasp.
		// See the carry method for more info on the mechanisms here.
		Lift lift = new Lift(item, random);
		return prepare(new Composed(lift, new Grasp(item, random), lift));
	}

	/**
	 * Put one item on another.
	 */
	public Option put(Soul item, Soul target) {
		// TODO What for meta??? Custom type????
		PlaceX place = new PlaceX(item, target, random);
		place.name = "put";
		return new Composed(place,
			prepare(new Grasp(item, random)),
			prepare(new Lift(item, random)),
			prepare(place),
			// TODO Lower before dropping?
			prepare(new Drop(item))
		);
	}

	/**
	 * Wraps and/or otherwise sets up the given option.
	 */
	public Option prepare(Option option) {
		return new TimeoutOption(option);
	}

	public Option rotate(Soul item) {
		RotateGrasp rotate = new RotateGrasp(item, random);
		return new Composed(rotate,
			prepare(rotate),
			// TODO Do we need a specialized life that can see how far?
			prepare(new Lift(item, random, 10)),
			prepare(new Drop(item))
		);
	}

}
