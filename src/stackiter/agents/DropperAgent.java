package stackiter.agents;

import java.util.*;

import stackiter.sim.*;

/**
 * Drops blocks randomly over the ground.
 */
public class DropperAgent extends BasicAgent {

	private Item target;

	@Override
	public void act() {
		if (target == null) {
			List<Item> options = new ArrayList<Item>();
			for (Item item : getWorld().getItems()) {
				if (!item.isAlive()) {
					options.add(item);
				}
			}
		}
	}

}
