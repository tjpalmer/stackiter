package stackiter.agents;

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
	private static final double CHANCE_OF_CENTER = 0.5;

	/**
	 * The focused item to be delivered.
	 */
	private Soul cargo;

	/**
	 * Our options factory.
	 */
	private Options options;

	/**
	 * Where the cargo should be delivered.
	 */
	private Soul pad;

	/**
	 * Used for anything random here.
	 */
	private Random random;

	public BuilderOptionAgent(Random random) {
		options = new Options();
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
				Point2D goal = point(0, 30);
				if (pad != null) {
					// We have a destination, so target it.
					// TODO Could see if anything's above it and go above those,
					// TODO but eh.
					Item padItem = state.items.get(pad);
					if (padItem == null) {
						// Lost the pad.
						pad = null;
					} else {
						goal = added(state.items.get(pad).getPosition(), goal);
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
		return option;
	}

	private void chooseCargo(State state) {
		// Reset the plan.
		cargo = pad = null;
		// Pick randomly for now.
		// TODO Care about what's on what?
		List<Item> items = list(state.items.values());
		if (!items.isEmpty()) {
			cargo = items.remove(random.nextInt(items.size())).getSoul();
			// Now pick a landing pad from those remaining.
			if (random.nextDouble() > CHANCE_OF_CENTER && !items.isEmpty()) {
				// TODO Could remove it from the list for consistency.
				pad = items.get(random.nextInt(items.size())).getSoul();
			}
		}
	}

}
