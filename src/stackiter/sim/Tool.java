package stackiter.sim;

/**
 * Represents agent presence in the world, for example the mouse pointer.
 */
public class Tool extends BasicItem {

	private ToolMode mode;

	public ToolMode getMode() {
		return mode;
	}

	public void setMode(ToolMode mode) {
		this.mode = mode;
	}

}
