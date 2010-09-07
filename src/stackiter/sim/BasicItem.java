package stackiter.sim;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;

/**
 * Simple data storage for items.
 */
public class BasicItem implements Item {

	private double angle;

	private Color color = Color.WHITE;

	private Point2D extent = new Point2D.Double();

	private Point2D position = new Point2D.Double();

	public boolean contains(Point2D point) {
		return rectangle(position, extent).contains(point);
	}

	@Override
	public double getAngle() {
		return angle;
	}

	@Override
	public Rectangle2D getBounds() {
		// TODO Merge with Block#getBounds() sometime.
		Point2D extent = getExtent();
		Rectangle2D rectangle = rectangle(-extent.getX(), -extent.getY(), 2 * extent.getX(), 2 * extent.getY());
		return rectangle;
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public Point2D getExtent() {
		return extent;
	}

	@Override
	public Point2D getPosition() {
		return position;
	}

	/**
	 * Simple rectangle, but no rotation supported at present.
	 */
	@Override
	public void paint(Graphics2D graphics, AffineTransform worldRelDisplay) {
		graphics = copy(graphics);
		try {
			graphics.setColor(color);
			Rectangle2D rectangle = rectangle(position.getX(), position.getY(), extent.getX(), extent.getY());
			// TODO Support rotation.
			rectangle = applied(worldRelDisplay, rectangle);
			graphics.fill(rectangle);
		} finally {
			graphics.dispose();
		}
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setExtent(Point2D extent) {
		this.extent.setLocation(extent);
	}

	public void setPosition(Point2D position) {
		this.position.setLocation(position);
	}

}
