package stackiter.agents;

import static stackiter.sim.Util.*;
import static stackiter.agents.ActionAgents.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import stackiter.agents.ActionAgents.Action;
import stackiter.sim.*;

public class BuilderAgent extends BasicAgent {

	/**
	 * The current subaction.
	 */
	private Action action;

	/**
	 * The focused item to be delivered.
	 */
	private Item cargo;

	//	/**
	//	 * The destination or landing pad for the current cargo.
	//	 *
	//	 * TODO Choose and use.
	//	 */
	//	private Item pad;

	private Tool tool;

	@Override
	public void act() {
		// Pick a new cargo item, if we don't already have one.
		List<Soul> itemSouls = getWorld().getItemSouls();
		if (cargo == null || !itemSouls.contains(cargo.getSoul())) {
			chooseCargo();
			if (cargo != null) {
				action = new Grasp(getWorld(), tool, cargo);
			}
		}
		if (action != null) {
			if (action.done()) {
				if (action instanceof Grasp) {
					action = new Carry(getWorld(), tool, point(0, 20));
				} else if (action instanceof Carry) {
					action = new Drop(getWorld(), tool);
				} else if (action instanceof Drop) {
					// Start over.
					action = null;
					cargo = null;
				}
			}
		}
		if (action != null) {
			action.act();
		}
	}

	private void chooseCargo() {
		// Randomly pick one for now.
		List<Item> items = listGraspableItems();
		if (!items.isEmpty()) {
			cargo = items.get(getRandom().nextInt(items.size()));
		}
	}

	private Random getRandom() {
		return getWorld().getTray().getRandom();
	}

	@Override
	protected void init() {
		tool = getWorld().addTool();
		tool.setColor(Color.ORANGE);
	}

	private List<Item> listGraspableItems() {
		// Find all tray items to consider choosing.
		List<Item> items = new ArrayList<Item>();
		Tray tray = getWorld().getTray();
		// Logic duped from World.getItems().
		for (Block block: tray.getItems()) {
			Block copied = block.clone();
			copied.setPosition(added(copied.getPosition(), tray.getAnchor()));
			items.add(copied);
		}
		// Also consider live items.
		Item ground = getWorld().getGround();
		for (Item item: getWorld().getItems()) {
			if (item.isAlive() && ground.getSoul() != item.getSoul()) {
				items.add(item);
			}
		}
		return items;
	}

}
