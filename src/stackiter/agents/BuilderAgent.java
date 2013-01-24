package stackiter.agents;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import stackiter.agents.ActionAgents.Action;
import stackiter.sim.*;

public class BuilderAgent extends BasicAgent {

	public static enum State {

	}

	/**
	 * The current subaction.
	 */
	private Action action;

	/**
	 * The focused item to be delivered.
	 */
	private Item cargo;

	/**
	 * The destination or landing pad for the current cargo.
	 */
	private Item pad;

	private Tool tool;

	@Override
	public void act() {
		// Pick a new cargo item, if we don't already have one.
		if (cargo == null || !getItemSouls().contains(cargo.getSoul())) {
			chooseCargo();
			action = new ActionAgents.Grasp(getWorld(), tool, cargo);
		}
		if (action.done()) {
			if (action instanceof ActionAgents.Grasp) {
				//
			}
		}
		action.act();
	}

	private void chooseCargo() {
		// Choose new cargo.
		// Find all tray items to consider choosing.
		List<Item> trayItems = new ArrayList<Item>();
		Tray tray = getWorld().getTray();
		// Logic duped from World.getItems().
		for (Block block: tray.getItems()) {
			Block copied = block.clone();
			copied.setPosition(added(copied.getPosition(), tray.getAnchor()));
			trayItems.add(copied);
		}
		// Randomly pick one for now.
		cargo = trayItems.get(getRandom().nextInt(trayItems.size()));
	}

	private List<Soul> getItemSouls() {
		List<Soul> itemSouls = new ArrayList<Soul>();
		for (Item item: getWorld().getItems()) {
			itemSouls.add(item.getSoul());
		}
		return itemSouls;
	}

	private Random getRandom() {
		return getWorld().getTray().getRandom();
	}

	@Override
	protected void init() {
		tool = getWorld().addTool();
		tool.setColor(Color.ORANGE);
	}

}
