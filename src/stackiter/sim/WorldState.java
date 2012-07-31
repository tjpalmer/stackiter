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
	 */
	public Map<Soul, Item> items = new LinkedHashMap<Soul, Item>();

	public double simTime;

	public long steps;

	public WorldState clone() {
		try {
			WorldState result = (WorldState)super.clone();
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

}
