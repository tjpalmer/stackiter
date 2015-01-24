package stackiter.agents;

import static java.lang.Math.*;
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
	 * Lifts enough to be above all blocks not already above it.
	 * Well, it also needs to get at least above the target.
	 */
	public static class AboveLift extends Lift {

		public AboveLift(Soul item, Random random) {
			super(item, random);
		}

		@Override
		protected void chooseGoal(State state) {
			Item item = state.items.get(this.item);
			Rectangle2D bounds = applied(item.getTransform(), item.getBounds());
			double minY = bounds.getMinY();
			// In finding the top, include the item to be moved.
			// Just get above everything.
			// Because the lift amount is only selected once, there's no fear of
			// infinite ascent chasing a carried block.
			double topY = 0;
			for (Item other: state.items.values()) {
				Rectangle2D otherBounds =
					applied(other.getTransform(), other.getBounds());
				topY = max(otherBounds.getMaxY(), topY);
			}
			// Add some units to be on the likely safe side.
			amount = topY + 5 - minY;
			// Now let super take it from here.
			super.chooseGoal(state);
		}

	}

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
		private static final double DEFAULT_DELAY_MEAN = 10.0;

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

		/**
		 * For easy override.
		 */
		public String name = "delay";

		public Delay(Random random) {
			this(random, DEFAULT_DELAY_MEAN);
		}

		public Delay(Random random, double delayMean) {
			beginTime = Double.POSITIVE_INFINITY;
			// Loop to force at least some wait.
			// This probably pushes the mean up some, but oh well.
			do {
				delayDuration =
					DELAY_DEVIATION * random.nextGaussian() + delayMean;
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
			return new Meta(name);
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

		public double deviation;

		public Soul item;

		/**
		 * Used for noisy actions.
		 */
		public Random random;

		/**
		 * Nothing null.
		 */
		public Grasp(Soul item, Random random) {
			if (item == null) {
				throw new RuntimeException("Null item.");
			}
			this.item = item;
			this.random = random;
			// Manually determined default.
			deviation = 0.5;
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
					Point2D graspPoint = chooseGraspPoint(liveItem);
					// Limited number of loops to avoid the infinite I've seen.
					// The actual number is arbitrary, though.
					boolean done = false;
					for (int i = 0; i < 30; i++) {
						// Within the loop, always start with the initial goal.
						Point2D noisyPoint = added(
							graspPoint,
							point(
								deviation * random.nextGaussian(),
								deviation * random.nextGaussian()
							)
						);
						if (liveItem.contains(noisyPoint)) {
							graspPoint = noisyPoint;
							done = true;
							break;
						}
					}
					if (!done) {
						// Failed to grasp for some reason. Just fail out.
						System.err.println(
							"Failed to grasp in " + liveItem.getBounds()
						);
						return null;
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

		/**
		 * Override to customize grasp mean.
		 * Gaussian noise based on deviation will be added.
		 * This method might also choose to modify the deviation, by the way.
		 */
		public Point2D chooseGraspPoint(Item item) {
			return item.getPosition();
		}

		@Override
		public boolean done(State state) {
			Item graspedItem = state.graspedItem;
			return graspedItem != null && graspedItem.getSoul() == item;
		}

		@Override
		public Meta meta() {
			return new Meta("grasp", item);
		}

		@Override
		public String toString() {
			return "Grasp(" + item + ")";
		}

	}

	/**
	 * Allows sequenced options as also suggested by Precup et al. (1998).
	 */
	public static class If implements Option {

		public static interface Condition {
			boolean check(State state);
		}

		Condition condition;

		Option falseCase;

		Option trueCase;

		Option option;

		Provider metaProvider;

		public If(
			Meta.Provider metaProvider,
			Condition condition,
			Option trueCase,
			Option falseCase
		) {
			this.condition = condition;
			this.metaProvider = metaProvider;
			this.falseCase = falseCase;
			this.trueCase = trueCase;
			// And be explicit.
			this.option = null;
		}

		@Override
		public Action act(State state) {
			if (option == null) {
				// Check just once up front.
				// For cases I care about, we don't want to reconsider the
				// condition once action is underway.
				option = condition.check(state) ? trueCase : falseCase;
			}
			return option.act(state);
		}

		@Override
		public boolean done(State state) {
			return option != null && option.done(state);
		}

		@Override
		public Meta meta() {
			return metaProvider.meta();
		}

	}

	public static class IsIsolated implements If.Condition {

		Soul item;

		public IsIsolated(Soul item) {
			this.item = item;
		}

		@Override
		public boolean check(State state) {
			// TODO What if it's null?
			Item item = state.items.get(this.item);
			List<Item> others = listOverlappers(item, state.items.values());
			return others.isEmpty();
		}

	}

	/**
	 * Move an item horizontally to place it above another.
	 */
	public static class Isolate extends DeferredGoalCarry {

		/**
		 * Just a convenience for our sorting algorithm to find open space.
		 */
		static class Edge implements Comparable<Edge> {
			enum Side {BEGIN, END}
			Side side;
			double x;
			public Edge(Side side, double x) {
				this.side = side;
				this.x = x;
			}
			@Override
			public int compareTo(Edge other) {
				return Double.compare(x, other.x);
			}
			@Override
			public String toString() {
				return "Edge(" + side + ", " + x + ")";
			}
		}

		public Isolate(Soul item, Random random) {
			super(item, random);
			name = "isolate";
		}

		@Override
		protected void chooseGoal(State state) {
			// TODO What if it's null?
			Item item = state.items.get(this.item);
			// First get a sorted list of edges of other items.
			// TODO Also exclude things that might be atop this item?
			List<Edge> edges = new ArrayList<Edge>();
			for (Item other: state.items.values()) {
				if (other == item) continue;
				Rectangle2D bounds =
					applied(other.getTransform(), other.getBounds());
				// Min and max x should be different unless we're zero width.
				// That shouldn't happen. TODO Assert against it?
				edges.add(new Edge(Edge.Side.BEGIN, bounds.getMinX()));
				edges.add(new Edge(Edge.Side.END, bounds.getMaxX()));
			}
			Collections.sort(edges);
			// Loop through for the biggest gap, and record the middle of it.
			double biggestGap = 0.0;
			double gapX = 0.0;
			int depth = 0;
			// Deal with extremeties only if we don't find a big enough gap.
			boolean inGap = false;
			double lastX = Double.NEGATIVE_INFINITY;
			for (Edge edge: edges) {
				if (inGap) {
					// Should be a begin. TODO Assert this?
					double gap = edge.x - lastX;
					if (gap > biggestGap) {
						biggestGap = gap;
						gapX = lastX + gap / 2;
					}
				}
				depth += edge.side == Edge.Side.BEGIN ? 1 : -1;
				inGap = depth == 0;
				if (inGap) {
					lastX = edge.x;
				}
			}
			// See if the gap is big enough for our item.
			double size =
				applied(item.getTransform(), item.getBounds()).getWidth();
			// How much space do we prefer extra on each side of our block.
			double wiggleRoom = 2;
			if (biggestGap < size + 2 * wiggleRoom) {
				// Not big enough to fit with at least a small margin.
				// Find the extremity nearest zero.
				double minX = edges.get(0).x;
				double maxX = edges.get(edges.size() - 1).x;
				// If I have lots of space, give more of a gap perhaps than
				// what I'm willing to put up with for slotting something in.
				double offset = 4;
				if (abs(minX) < abs(maxX)) {
					// Go min.
					gapX = minX - offset - size / 2;
				} else {
					// Go max.
					gapX = maxX + offset + size / 2;
				}
			}
			// Noise will already have been added to the existing "0" goal.
			goal = added(goal, point(gapX, item.getPosition().getY()));
		}

		@Override
		public Meta meta() {
			return new Meta(name, item);
		}

	}

	/**
	 * A constrained carry that just lifts vertically.
	 */
	public static class Lift extends DeferredGoalCarry {

		/**
		 * Amount to lift by.
		 */
		public double amount;

		public Lift(Soul item, Random random) {
			// For default, just go to a rather tall height.
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
	 * A constrained carry that lowers to just above the nearest beneath.
	 */
	public static class Lower extends DeferredGoalCarry {

		/**
		 * Amount to lift by.
		 */
		public double amount;

		/**
		 * If specified, use its x for the goal rather than the x of the moved
		 * item.
		 */
		private Soul target;

		/**
		 * An alternative goal at a specific coordinate.
		 */
		private Double x;

		public Lower(Soul item, Random random) {
			super(item, random);
			name = "lower";
		}

		public Lower(Soul item, Soul target, Random random) {
			this(item, random);
			this.target = target;
		}

		public Lower(Soul item, double x, Random random) {
			this(item, random);
			this.x = x;
		}

		@Override
		protected void chooseGoal(State state) {
			Item item = state.items.get(this.item);
			Rectangle2D bounds = applied(item.getTransform(), item.getBounds());
			// The ground level is 0.
			double topY = 0;
			List<Item> others = listOverlappers(item, state.items.values());
			for (Item other: others) {
				Rectangle2D otherBounds =
					applied(other.getTransform(), other.getBounds());
				// Find the highest point among such items.
				// Note that the highest point might not be beneath us, but
				// this should be good enough most of the time.
				topY = max(topY, otherBounds.getMaxY());
			}
			// Give extra space for some safety margin.
			// We might still sometimes choose to slam down, but that's life.
			double dropGap = 2;
			double distance = bounds.getMinY() - (topY + dropGap);
			if (distance < 0) {
				// Already below the top???
				// This shouldn't be common, given our common behavior, but just
				// just we're good in this case.
				distance = 0;
			}
			// Figure out if we base x on target or on item.
			Point2D offset;
			Item targetItem = target == null ? null : state.items.get(target);
			if (targetItem != null) {
				offset = point(
					targetItem.getPosition().getX(), item.getPosition().getY()
				);
			} else if (x != null) {
				offset = point(x, item.getPosition().getY());
			} else {
				// Just keep our current x.
				offset = item.getPosition();
			}
			offset = added(offset, point(0, -distance));
			// Noise will already have been added to the existing "0" goal.
			goal = added(goal, offset);
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
	 * Move an item horizontally to position it at a specific x coordinate.
	 */
	public static class Move extends DeferredGoalCarry {

		private double x;

		public Move(Soul item, double x, Random random) {
			super(item, random);
			this.x = x;
			name = "move";
		}

		@Override
		protected void chooseGoal(State state) {
			// TODO What if it's is null?
			Item item = state.items.get(this.item);
			// Noise will already have been added to the existing "0" goal.
			goal = added(goal, point(x, item.getPosition().getY()));
		}

		@Override
		public Meta meta() {
			return new Meta(name, item, x);
		}

	}

	/**
	 * Attempts to rotate the item by grabbing a side.
	 */
	public static class RotateGrasp extends Grasp {

		public RotateGrasp(Soul item, Random random) {
			super(item, random);
		}

		@Override
		public Point2D chooseGraspPoint(Item item) {
			// Presuming rotational symmetry at 180, see if we are rotated more
			// 0 or 90.
			Point2D point;
			// Tailored a lot on the fraction here to be as free but as stable
			// as possible.
			double offsetFraction = 0.7;
			double offsetSize;
			double angle = item.getAngle();
			if (angle < 0) {
				// Always grab from global right.
				offsetFraction *= -1;
			}
			if (abs(abs(angle) - 0.5) < 0.25) {
				// Rotated 90. Vertical is horizontal.
				offsetSize = item.getExtent().getY();
				point = point(0, -offsetSize * offsetFraction);
			} else {
				// Original orientation. Grab at right to swing it.
				offsetSize = item.getExtent().getX();
				point = point(offsetSize * offsetFraction, 0);
			}
			// Now rotate the point with the item.
			AffineTransform rotation =
				AffineTransform.getRotateInstance(angle * PI);
			point = added(applied(rotation, point), item.getPosition());
			// Update the deviation to be some fraction of the offset.
			// Don't let it be too big, or else this might fail too often.
			deviation = 0.025 * offsetSize;
			return point;
		}

		@Override
		public Meta meta() {
			return new Meta("rotate", item);
		}

		@Override
		public String toString() {
			return "Rotate(" + item + ")";
		}

	}

	/**
	 * Lifts just enough for a rotate, or at least that's the idea.
	 * Since it's staying in place, it doesn't need lifted enough to clear other
	 * objects.
	 */
	public static class RotateLift extends Lift {

		public RotateLift(Soul item, Random random) {
			super(item, random);
		}

		@Override
		protected void chooseGoal(State state) {
			Item item = state.items.get(this.item);
			// We're already up the current vertical, so add the new vertical.
			if (abs(abs(item.getAngle()) - 0.5) < 0.25) {
				// Rotated 90. Vertical is horizontal, but will be vertical.
				amount = item.getExtent().getY();
			} else {
				// Original orientation. The width will be vertical.
				amount = item.getExtent().getX();
			}
			// I keep wanting to think I should add twice for diameter, but for
			// some reason that's too much most of the time.
			// Still, not multiplying seems to fail too often, and with the
			// Lower option, we can get things low enough often enough for safe
			// setdown.
			// So go ahead and double.
			amount *= 2;
			// Now let super take it from here.
			super.chooseGoal(state);
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
			if (abs(item.getAngularVelocity()) >= EPSILON) {
				return false;
			}
		}
		// Everything's at rest (enough).
		return true;
	}

	/**
	 * Return a list of those items which overlap item horizontally.
	 */
	private static List<Item> listOverlappers(
		Item item, Collection<Item> items
	) {
		List<Item> overlappers = new ArrayList<Item>();
		Rectangle2D bounds = applied(item.getTransform(), item.getBounds());
		double minX = bounds.getMinX();
		double maxX = bounds.getMaxX();
		for (Item other: items) {
			if (other == item) continue;
			Rectangle2D otherBounds =
				applied(other.getTransform(), other.getBounds());
			if (
				between(otherBounds.getMinX(), minX, maxX) ||
				between(otherBounds.getCenterX(), minX, maxX) ||
				between(otherBounds.getMaxX(), minX, maxX)
			) {
				// Some kind of x overlap exists.
				overlappers.add(other);
			}
		}
		return overlappers;
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

	public Option done() {
		// Short delay with custom name.
		// This is a subjective action, indicating that the agent thinks its job
		// is done.
		// It's the client software's job to interpret this in any special way.
		// For example, the client might choose to issue a clear or some such.
		// By coming through here, though, we do get a delay, and the action
		// ends up in the logs.
		Delay done = new Delay(random, 1.0);
		done.name = "done";
		return prepare(done);
	}

	public Option drop(State state) {
		return prepare(new Drop(state));
	}

	public Option grasp(Soul item) {
		return prepare(new Grasp(item, random));
	}

	/**
	 * Put an item on an empty spot on the table, if possible.
	 */
	public Option isolate(Soul item) {
		Isolate isolate = new Isolate(item, random);
		return new If(isolate,
			// Only bother to move things if the item isn't already alone.
			new IsIsolated(item),
			// Use done here for a canonically short delay, since we'll always
			// want a short delay there, too.
			done(),
			new Composed(isolate,
				prepare(new Grasp(item, random)),
				prepare(new AboveLift(item, random)),
				prepare(isolate),
				// TODO Tie the goal x from isolate into this somehow?
				prepare(new Lower(item, random)),
				prepare(new Drop(item))
			)
		);
	}

	public Option lift(Soul item) {
		// To simplify sequencing, make lift also grasp.
		// See the carry method for more info on the mechanisms here.
		Lift lift = new Lift(item, random);
		return prepare(new Composed(lift, new Grasp(item, random), lift));
	}

	/**
	 * Place an item on a specific x coordinate.
	 */
	public Option move(Soul item, double x) {
		Move move = new Move(item, x, random);
		return new Composed(move,
			prepare(new Grasp(item, random)),
			prepare(new AboveLift(item, random)),
			prepare(move),
			prepare(new Lower(item, x, random)),
			prepare(new Drop(item))
		);
	}

	/**
	 * Put one item on another.
	 */
	public Option put(Soul item, Soul target) {
		PlaceX place = new PlaceX(item, target, random);
		place.name = "put";
		return new Composed(place,
			prepare(new Grasp(item, random)),
			prepare(new AboveLift(item, random)),
			prepare(place),
			prepare(new Lower(item, target, random)),
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
			prepare(new RotateLift(item, random)),
			// TODO Option to remember where item started? Does it matter?
			prepare(new Lower(item, random)),
			prepare(new Drop(item))
		);
	}

}
