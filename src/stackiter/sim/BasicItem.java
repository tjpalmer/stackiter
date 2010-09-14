package stackiter.sim;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;

/**
 * Simple data storage for items.
 *
 * TODO Consider making Block and others subclass this for convenience.
 */
public class BasicItem implements Item {

	private boolean alive;

	private double angle;

	private Color color = Color.WHITE;

	private Point2D extent = new Point2D.Double();

	private Object soul = new Object();

	private Point2D position = new Point2D.Double();

	@Override
	public Item clone() {
		BasicItem copied;
		try {
			copied = (BasicItem)super.clone();
		} catch (Exception e) {
			// Should never happen.
			throw new RuntimeException(e);
		}
		// Make sure we actually have copies of the mutable objects.
		copied.extent = copy(extent);
		copied.position = copy(position);
		return copied;
	}

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

	@Override
	public Object getSoul() {
		return soul;
	}

	@Override
	public boolean isAlive() {
		return alive;
	}

	/**
	 * Simple rectangle, but no rotation supported at present.
	 */
	@Override
	public void paint(Graphics2D graphics, AffineTransform worldRelDisplay) {
		graphics = copy(graphics);
		try {
			graphics.setColor(color);
			Rectangle2D rectangle = rectangle(getPosition(), getExtent());
			// TODO Support rotation.
			rectangle = applied(worldRelDisplay, rectangle);
			graphics.fill(rectangle);
		} finally {
			graphics.dispose();
		}
	}

	@Override
	public void setAlive(boolean alive) {
		this.alive = alive;
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
