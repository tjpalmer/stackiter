package stackiter.agents;

import java.awt.*;
import java.awt.geom.*;

import stackiter.sim.*;

/**
 * Occasionally clears the world.
 */
public class ClearerAgent extends BasicAgent {

	private Point2D clearerPosition;

	private int count;

	private Tool tool;

	private final int delay;

	public ClearerAgent(double delaySeconds) {
		this.delay = (int)(100 * delaySeconds);
	}

	@Override
	public void act() {
		// This assumes the clearer doesn't move.
		if (clearerPosition == null) {
			Clearer clearer = getWorld().getClearer();
			if (clearer != null) {
				clearerPosition = clearer.getPosition();
				tool.setPosition(clearerPosition);
			}
		}
		// Stackiter UI generally runs at about 100 Hz.
		// TODO Be better about sim time?
		if (count == delay) {
			tool.setMode(ToolMode.GRASP);
			count = 0;
		} else {
			tool.setMode(ToolMode.INACTIVE);
		}
		count++;
	}

	@Override
	protected void init() {
		tool = getWorld().addTool();
		// TODO Should we stylesheet agent colors instead?
		tool.setColor(Color.RED);
	}

}
