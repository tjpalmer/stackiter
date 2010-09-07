package stackiter.sim;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;

/**
 * Represents agent presence in the world, for example the mouse pointer.
 */
public class Tool extends BasicItem {

	public Tool() {
		setExtent(point(1, 1));
	}

	private ToolMode mode;

	public ToolMode getMode() {
		return mode;
	}

	@Override
	public void paint(Graphics2D graphics, AffineTransform worldRelDisplay) {
		graphics = copy(graphics);
		try {
			// Find the bounds for the cross-hairs.
			Rectangle2D outer = applied(worldRelDisplay, rectangle(getPosition(), getExtent()));
			Rectangle2D inner = applied(worldRelDisplay, rectangle(getPosition(), scaled(0.2, getExtent())));
			// Draw them darker.
			graphics.setColor(getColor().darker());
			graphics.setStroke(new BasicStroke(3));
			drawCrosshairs(graphics, outer, inner);
			// Draw them brighter.
			graphics.setColor(getColor());
			graphics.setStroke(new BasicStroke(1));
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
