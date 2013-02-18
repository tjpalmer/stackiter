package stackiter.agents;

import static java.lang.Math.*;
import static stackiter.sim.Util.*;

import java.awt.geom.*;
import java.util.*;

import stackiter.sim.*;

/**
 * Does a bad job of building a stack of blocks through the option agent
 * framework.
 * Unlike the BuilderAgent, this one only gets asked for an option selection
 * after the previous one finishes.
 * <p>
 * TODO Smarter stacking.
 */
public class BuilderOptionAgent implements OptionAgent {


	/**
	 * To keep the building near the center of the table, sometimes just carry
	 * things there instead of over other blocks.
	 */
	private static final double CHANCE_OF_GOAL_POINT = 0.25;

	/**
	 * The focused item to be delivered.
	 */
	private Soul cargo;

	/**
	 * Where the cargo should be delivered.
	 * If null, the goal is just an absolute coordinate.
	 */
	private Soul goalItem;

	/**
	 * The point from which the cargo should be dropped.
	 * This is relative if goalItem is non-null.
	 */
	private Point2D goalPoint;

	/**
	 * Our options factory.
	 */
	private Options options;

	/**
	 * Used for anything random here.
	 */
	private Random random;

	public BuilderOptionAgent(Random random) {
		options = new Options(random);
		this.random = random;
	}

	@Override
	public Option act(State state) {
		Option option = null;
		// First, see if we have a plan.
		if (cargo == null || !state.items.containsKey(cargo)) {
			chooseCargo(state);
			option = options.grasp(cargo);
		} else if (state.graspedItem != null) {
			if (state.graspedItem.getSoul() != cargo) {
				// Whoa nelly! How did that happen?
				option = options.drop(state);
			} else {
				// Okay. We have the item grasped.
				// Pretend we need to carry it. We might need to.
				Point2D goal = goalPoint;
				if (goalItem != null) {
					// We have a destination, so target it.
					// TODO Could see if anything's above it and go above those,
					// TODO but eh.
					Item liveGoalItem = state.items.get(goalItem);
					if (liveGoalItem == null) {
						// Lost the landing pad.
						goalItem = null;
					} else {
						Point2D goalPosition =
							state.items.get(goalItem).getPosition();
						// Use our chosen goal point as an offset.
						goal = added(goalPosition, goalPoint);
					}
				}
				option = options.carry(goal);
				// See if we are already there.
				// We could do our own math, but delegating to Carry allows its
				// own logic to be used consistently.
				if (option.done(state)) {
					// Well, that's done, actually, so drop it.
					option = options.drop(state);
					// Next time we get asked (when drop is done), we'll be
					// ready for a new round.
					cargo = null;
				}
			}
		}
		// Some option is selected by this point.
		System.out.println(option);
		return option;
	}

	private static double[] buildCdf(List<Item> items) {
		// Make the ones near the center the most likely choices, so's to
		// concentrate the action.
		Gaussian distribution = new Gaussian(0.0, 20.0);
		double[] weights = new double[items.size()];
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			weights[i] = distribution.density(item.getPosition().getX());
		}
		// Get the sum.
		double sum = 0.0;
		for (double weight: weights) {
			sum += weight;
		}
		// Accumulate, starting from the second.
		for (int w = 1; w < weights.length; w++) {
			weights[w] += weights[w - 1];
		}
		// And build the CDF.
		for (int w = 0; w < weights.length; w++) {
			weights[w] /= sum;
		}
		return weights;
	}

	private void chooseCargo(State state) {
		// Reset the plan.
		cargo = goalItem = null;
		// Pick randomly for now.
		// TODO Care about what's on what?
		List<Item> items = list(state.items.values());
		if (!items.isEmpty()) {
			cargo = items.remove(random.nextInt(items.size())).getSoul();
			// Now pick a destination.
			if (random.nextDouble() < CHANCE_OF_GOAL_POINT || items.isEmpty()) {
				// Pick a global goal point.
				while (true) {
					// Most weight will be between -20 and 20 here.
					double goalX = 10.0 * random.nextGaussian();
					// Rejection sampling, to keep things sane.
					// TODO Info on table size for wrap around?
					// TODO Or allow just a but beyond items?
					if (abs(goalX) < 40.0) {
						goalPoint = point(goalX, dropHeight());
						break;
					}
				}
			} else {
				// Pick from available items.
				double[] itemCdf = buildCdf(items);
				double choice = random.nextDouble();
				// Go up from the bottom, finding the first chunk covering our
				// random value.
				// The last value is always 1, so we'll get something by the
				// end.
				// TODO Extract all this weighted sampling logic.
				for (int i = 0; i < itemCdf.length; i++) {
					if (choice < itemCdf[i]) {
						// Found it.
						// TODO Could remove it from the list for consistency
						// TODO with how we remove the cargo after selection.
						goalItem = items.get(i).getSoul();
						// Goal point is now relative here.
						// Always aim for the center.
						// Let noise apply elsewhere.
						goalPoint = point(0.0, dropHeight());
						break;
					}
				}
			}
		}
	}

	/**
	 * Some variety in target drop height should be healthy.
	 */
	private double dropHeight() {
		// Too high is wasteful, and too low risks collisions with the goal.
		// TODO Better tracking of what's a safe minimum.
		return randInRange(random, 10, 40);
	}

}
