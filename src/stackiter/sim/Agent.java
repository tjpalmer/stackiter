package stackiter.sim;

/**
 * A way of stuffing agent control into someone else's top-level loop.
 */
public interface Agent {

	/**
	 * Called before world update. Should run fast!
	 */
	void act();

	World getWorld();

	/**
	 * Called after world update. Should run fast!
	 *
	 * The main point of this method is to provide world state before any delay
	 * in the update loop. So this really only matters if the agent performs
	 * background processing between steps.
	 */
	void sense();

	/**
	 * Any initial changes to the world should be done inside this call.
	 * Adding a tool is one common option.
	 */
	void setWorld(World world);

}
