package stackiter.sim;

import java.awt.*;
import java.awt.geom.*;

public interface Item extends Cloneable {

	/**
	 * Should provide a deep copy.
	 */
	public Item clone();

	public boolean contains(Point2D point);

	/**
	 * @return angle in rats (radians / pi).
	 */
	public double getAngle();

	public double getAngularAcceleration();

	public double getAngularVelocity();

	/**
	 * Bounds relative to the object transform.
	 */
	public Rectangle2D getBounds();

	public Color getColor();

	/**
	 * TODO Extent can be convenient, but it assumes a geometrically centered
	 * TODO position. Reconsider this.
	 *
	 * TODO Change all these getters to receiving a point to fill????
	 */
	public Point2D getExtent();

	public Point2D getLinearAcceleration();

	public Point2D getLinearJerk();

	public Point2D getLinearVelocity();

	public Point2D getPosition();

	public AffineTransform getTransform();

	/**
	 * A way of tracking object identity across copies (and time). When exposing
	 * world state, you aren't guaranteed to get original objects, but the soul
	 * should be the same.
	 *
	 * Depending on your context, though, it might be unfair to use this. Humans
	 * have to figure this out the hard way, and some agents might ought to as
	 * well.
	 *
	 * This is also easier to manage than ID generation.
	 */
	public Soul getSoul();

	/**
	 * Whether the item interacts physically in the world with other items.
	 */
	public boolean isAlive();

	public void paint(Graphics2D graphics);

	public void setAlive(boolean alive);

	public void setAngle(double angle);

	public void setAngularAcceleration(double angularAcceleration);

	public void setAngularVelocity(double angularVelocity);

	public void setColor(Color color);

	public void setExtent(Point2D extent);

	public void setLinearAcceleration(Point2D linearAcceleration);

	/**
	 * TODO Do I really care about explicit jerk?
	 */
	public void setLinearJerk(Point2D linearJerk);

	public void setLinearVelocity(Point2D linearVelocity);

	public void setPosition(Point2D position);

}
