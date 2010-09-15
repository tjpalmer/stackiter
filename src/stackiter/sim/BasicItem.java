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

	private double angularVelocity;

	private Color color = Color.WHITE;

	private Point2D extent = point();

	private Point2D linearVelocity = point();

	private Object soul = new Object();

	private Point2D position = point();

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

	@Override
	public boolean contains(Point2D point) {
		return rectangle(position, extent).contains(point);
	}

	@Override
	public double getAngle() {
		return angle;
	}

	@Override
	public double getAngularVelocity() {
		return angularVelocity;
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
	public Point2D getLinearVelocity() {
		return linearVelocity;
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

	@Override
	public void setAngle(double angle) {
		this.angle = angle;
	}

	@Override
	public void setAngularVelocity(double angularVelocity) {
		this.angularVelocity = angularVelocity;
	}

	@Override
	public void setColor(Color color) {
		this.color = color;
	}

	@Override
	public void setExtent(Point2D extent) {
		this.extent.setLocation(extent);
	}

	@Override
	public void setLinearVelocity(Point2D linearVelocity) {
		this.linearVelocity.setLocation(linearVelocity);
	}

	@Override
	public void setPosition(Point2D position) {
		this.position.setLocation(position);
	}

}
