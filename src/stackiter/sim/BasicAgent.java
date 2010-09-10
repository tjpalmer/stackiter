package stackiter.sim;

public class BasicAgent implements Agent {

	private World world;

	@Override
	public void act() {
		// Do nothing by default
	}

	public World getWorld() {
		return world;
	}

	/**
	 * For setting up once the world is attached.
	 */
	protected void init() {
		// Nothing by default.
	}

	@Override
	public void sense() {
		// Do nothing by default.
	}

	public void setWorld(World world) {
		this.world = world;
		init();
	}

}
