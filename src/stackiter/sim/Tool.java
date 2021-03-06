package stackiter.sim;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;

/**
 * Represents agent presence in the world, for example the mouse pointer.
 */
public class Tool extends BasicItem {

	private ToolMode mode = ToolMode.INACTIVE;

	public Tool() {
		setExtent(point(1, 1));
	}

	@Override
	public Tool clone() {
		return (Tool)super.clone();
	}

	public ToolMode getMode() {
		return mode;
	}

	@Override
	public void paint(Graphics2D graphics) {
		graphics = copy(graphics);
		try {
			// Find the bounds for the cross-hairs.
			Rectangle2D outer = rectangle(getPosition(), getExtent());
			Rectangle2D inner = rectangle(getPosition(), scaled(0.2, getExtent()));
			// Base color on mode.
			// TODO Provide a parameter on active color?
			Color color = mode == ToolMode.INACTIVE ? getColor() : getColor().darker();
			// Draw them darker.
			graphics.setColor(color.darker());
			graphics.setStroke(new BasicStroke(0.2f));
			drawCrosshairs(graphics, outer, inner);
			// Draw them brighter.
			graphics.setColor(color);
			graphics.setStroke(new BasicStroke(0.05f));
			drawCrosshairs(graphics, outer, inner);
		} finally {
			graphics.dispose();
		}
	}

	private void drawCrosshairs(Graphics2D graphics, Rectangle2D outer, Rectangle2D inner) {
		Point2D center = point(inner.getCenterX(), inner.getCenterY());
		// Horizontals.
		graphics.draw(segment(inner.getMaxX(), center.getY(), outer.getMaxX(), center.getY()));
		graphics.draw(segment(inner.getMinX(), center.getY(), outer.getMinX(), center.getY()));
		// Verticals.
		graphics.draw(segment(center.getX(), inner.getMaxY(), center.getX(), outer.getMaxY()));
		graphics.draw(segment(center.getX(), inner.getMinY(), center.getX(), outer.getMinY()));
	}

	public void setMode(ToolMode mode) {
		this.mode = mode;
	}

}
