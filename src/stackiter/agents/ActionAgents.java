package stackiter.agents;

import static stackiter.sim.ToolMode.*;
import stackiter.sim.*;

/**
 * Set of temporally extended actions for higher level control.
 */
public abstract class ActionAgents {

	public static abstract class Action extends BasicAgent {
		public abstract boolean done();
	}

	public static class Grasp extends Action {

		public Item item;

		public Tool tool;

		/**
		 * Nothing null.
		 */
		public Grasp(World world, Tool tool, Item item) {
			this.item = item;
			this.tool = tool;
			// Do this last, to prepare for init, if wanted.
			setWorld(world);
		}

		@Override
		public void act() {
			if (done()) return;
			if (tool.getMode() != INACTIVE) {
				tool.setMode(INACTIVE);
				// TODO Can we move in the same time step without dragging the
				// TODO previous item?
			} else {
				tool.setPosition(item.getPosition());
				tool.setMode(GRASP);
			}
		}

		@Override
		public boolean done() {
			// TODO Work on soul level?
			Item graspedItem = getWorld().getGraspedItem(tool);
			return
				graspedItem != null && graspedItem.getSoul() == item.getSoul();
		}

	}

	private ActionAgents() {}

}
