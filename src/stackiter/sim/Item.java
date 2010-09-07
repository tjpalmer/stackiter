package stackiter.sim;

import java.awt.*;
import java.awt.geom.*;

public interface Item {

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

	public void paint(Graphics2D graphics, AffineTransform worldRelDisplay);

	public void setAngle(double angle);

	public void setColor(Color color);

	public void setExtent(Point2D extent);

	public void setPosition(Point2D position);

}
