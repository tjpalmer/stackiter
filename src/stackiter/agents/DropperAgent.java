package stackiter.agents;

import java.awt.*;
import java.util.*;
import java.util.List;

import stackiter.sim.*;

/**
 * Drops blocks randomly over the ground.
 */
public class DropperAgent extends BasicAgent {

	private Item target;

	private Tool tool;

	@Override
	public void act() {
		if (target == null) {
			List<Item> options = new ArrayList<Item>();
			for (Item item: getWorld().getItems()) {
				if (!item.isAlive()) {
					options.add(item);
				}
			}
		}
		// TODO Do something with tool.
	}

	@Override
	protected void init() {
		tool = getWorld().addTool();
		// TODO Should we stylesheet agent colors instead?
		tool.setColor(Color.GREEN);
	}

}
