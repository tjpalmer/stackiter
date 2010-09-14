package stackiter.sim;

import java.awt.*;
import java.awt.geom.*;

public interface Item extends Cloneable {

	/**
	 * Should provide a deep copy.
	 */
	public Item clone();

	public boolean contains(Point2D point);

	public double getAngle();

	/**
	 * Bounds relative to the object transform.
	 */
	public Rectangle2D getBounds();

	public Color getColor();

	/**
	 * TODO Extent can be convenient, but it assumes a geometrically centered
	 * TODO position. Reconsider this.
	 */
	public Point2D getExtent();

	public Point2D getPosition();

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
	public Object getSoul();

	/**
	 * Whether the item interacts physically in the world with other items.
	 */
	public boolean isAlive();

	public void paint(Graphics2D graphics, AffineTransform worldRelDisplay);

	public void setAlive(boolean alive);

	public void setAngle(double angle);

	public void setColor(Color color);

	public void setExtent(Point2D extent);

	public void setPosition(Point2D position);

}
