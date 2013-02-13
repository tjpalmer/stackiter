package stackiter.sim;

import java.util.*;

/**
 * A more standalone data representation of the state of the world, not tied to
 * live simulation or whatnot. It's useful for tracking state and for snapshots.
 * For now, it just tracks what we need for such cases.
 */
public class WorldState implements Cloneable {

	/**
	 * Whether the world represents a "cleared" state.
	 */
	public boolean cleared;

	/**
	 * LinkedHashMap preserves add order.
	 *
	 * TODO While it's nice to have automatic ids as objects (souls), they don't
	 * TODO exactly serialize well.
	 * TODO Having had a unique id generator to begin with might have been
	 * TODO nicer.
	 */
	public Map<Soul, Item> items = new LinkedHashMap<Soul, Item>();

	public double simTime;

	public long steps;

	/**
	 * Customized to deep-clone the items (but not the souls).
	 */
	public WorldState clone() {
		try {
			WorldState result = (WorldState)super.clone();
			// Deep clone the items.
			result.items = new LinkedHashMap<Soul, Item>(items.size());
			for (Item item: items.values()) {
				result.items.put(item.getSoul(), item.clone());
			}
			return result;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a clone, initialized for a new state.
	 */
	public WorldState cloneNew(long steps, double simTime) {
		WorldState result = clone();
		result.steps = steps;
		result.simTime = simTime;
		result.cleared = false;
		return result;
	}

	/**
	 * Fills in details from the given world.
	 * Leaves out the clearer or other widgets for now.
	 * Also leaves out the ground/table for now.
	 * TODO Reconsider omissions.
	 *
	 * For now, leaves cleared unspecified, as that's not clearly indicated by
	 * world objects.
	 */
	public void fillFrom(World world) {
		// Copy over items.
		// TODO Provide some mechanism for appending??
		items.clear();
		// Blocks don't include the clearer nor the ground/table.
		Iterable<Block> worldItems = world.getBlocks();
		items = new LinkedHashMap<Soul, Item>();
		for (Item item: worldItems) {
			items.put(item.getSoul(), item.clone());
		}

		// Time fields.
		simTime = world.getSimTime();
		// TODO Just expose step count directly????
		steps = Math.round(world.getSimTime() / world.getStepTime());
	}

}
