package stackiter.agents;

import java.awt.*;
import java.awt.geom.*;

import stackiter.sim.*;

import static stackiter.sim.Util.*;
import static stackiter.sim.ToolMode.*;

/**
 * A simpler type of agent that receives a data form of world state and returns
 * an option (in some ways, a subagent) to control the agent until the option is
 * completed.
 *
 * This is also related to SMDP (Semi-Markov Decision Process) perspectives,
 * although for now I have no reward signal baked in.
 */
public interface OptionAgent {

	/**
	 * Encapsulates action data.
	 * For now, that's just about the tool and might only ever be.
	 */
	static class Action {

		/**
		 * Set to true to clear the world.
		 *
		 * In some cases, this can indicate the end of an episode.
		 */
		public boolean clear;

		/**
		 * The prescribed tool state.
		 */
		public ToolState tool = new ToolState();

		/**
		 * Default tool state.
		 */
		public Action() {
			// Nothing to do.
		}

		/**
		 * Copy in the given tool state.
		 */
		public Action(ToolState toolState) {
			tool.fillFrom(toolState);
		}

	}

	/**
	 * A bridge from traditional Stackiter agents to option agents.
	 */
	static class Bridge extends BasicAgent {

		/**
		 * The option agent that makes the high level decisions.
		 */
		public OptionAgent agent;

		/**
		 * The currently active option. Nullable.
		 */
		public Option option;

		/**
		 * The tool being controlled by this agent.
		 * Created by default in init.
		 */
		public Tool tool;

		public Bridge(OptionAgent agent) {
			this.agent = agent;
		}

		@Override
		public void act() {
			// Gather up state information.
			State state = new State();
			state.fillFrom(getWorld(), tool);
			// See if we need to select a new option.
			if (option == null || option.done(state)) {
				// Note that the agent itself only chooses options at this
				// point.
				// Options do the rest.
				option = agent.act(state);
			}
			// Presume we allow at least one action.
			// If the option thinks it's already done, let it tell us that next
			// time around.
			if (option != null) {
				Action action = option.act(state);
				if (action != null) {
					action.tool.fillTo(tool);
					if (action.clear) {
						getWorld().clearBlocks();
					}
				}
			}
		}

		@Override
		protected void init() {
			if (tool == null) {
				tool = getWorld().addTool();
				tool.setColor(Color.ORANGE);
			}
		}

	}

	/**
	 * An option that makes low-level decisions.
	 */
	static interface Option {

		/**
		 * Chooses an action (for a tool) based on world state.
		 */
		public Action act(State state);

		/**
		 * Determines if the option is complete.
		 */
		public boolean done(State state);

	}

	/**
	 * Extends common world state with convenience tracking for a particular
	 * tool.
	 */
	static class State extends WorldState {

		/**
		 * The item being grasped by the tool, or null if no item is grasped.
		 * Tracking this explicitly help avoid some pain.
		 * If non-null, this object corresponds to one of the objects in the
		 * items map (pointer equality).
		 * <p>
		 * This isn't inside ToolState, since I use ToolState for both input and
		 * output, and a tool can't just say what item it wants to be grasping.
		 * It actually has to go and grasp it.
		 */
		public Item graspedItem;

		/**
		 * The current state of the tool being controlled by the agent whos
		 * viewpoint this is.
		 * <p>
		 * TODO Instead have a list of tools or whatnot and just give the soul
		 * TODO here?
		 */
		public ToolState tool = new ToolState();

		public void fillFrom(World world, Tool tool) {
			fillFrom(world);
			graspedItem = world.getGraspedItem(tool);
			this.tool.fillFrom(tool);
		}

	}

	/**
	 * Basic data for tool state, whether descriptive (state) or prescriptive
	 * (action).
	 */
	static class ToolState {

		/**
		 * Whether or not the tool is active (i.e., grasping, in most cases).
		 */
		boolean active;

		/**
		 * Where the tool is.
		 */
		Point2D position = point();

		public void fillFrom(Tool tool) {
			this.position.setLocation(tool.getPosition());
			this.active = tool.getMode() == GRASP;
		}

		public void fillFrom(ToolState toolState) {
			this.position.setLocation(toolState.position);
			this.active = toolState.active;
		}

		public void fillTo(Tool tool) {
			tool.getPosition().setLocation(position);
			tool.setMode(active ? GRASP : INACTIVE);
		}

	}

	/**
	 * The one thing an option agent does.
	 */
	Option act(State state);

}
