package stackiter;

import java.awt.*;
import java.awt.geom.*;

public class Util {

	/**
	 * TODO Make these "applied" since nondestructive?
	 */
	public static Point2D applied(AffineTransform transform, Point2D point) {
		return transform.transform(point, null);
	}

	public static Rectangle2D applied(AffineTransform transform, Rectangle2D rectangle) {
		// TODO Handle this manually more efficiently?
		Path2D path = new Path2D.Double(rectangle);
		path.transform(transform);
		return path.getBounds2D();
	}

	public static Point2D appliedInv(AffineTransform transform, Point2D point) {
		try {
			return transform.inverseTransform(point, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static AffineTransform copy(AffineTransform transform) {
		return (AffineTransform)transform.clone();
	}

	/**
	 * @return the center of the bounding rectangle.
	 */
	public static Point2D center(Shape shape) {
		Rectangle2D bounds = shape.getBounds2D();
		return point(bounds.getCenterX(), bounds.getCenterY());
	}

	/**
	 * Remember to dispose all graphics objects!!!
	 * @param graphics
	 * @return
	 */
	public static Graphics2D copy(Graphics graphics) {
		return (Graphics2D)graphics.create();
	}

	public static Point2D copy(Point2D point) {
		return (Point2D)point.clone();
	}

	/**
	 * @return the extent (half width, half height) of the bounding rectangle.
	 */
	public static Point2D extent(Shape shape) {
		Rectangle2D bounds = shape.getBounds2D();
		return point(bounds.getWidth() / 2, bounds.getHeight() / 2);
	}

	public static void invert(AffineTransform transform) {
		try {
			transform.invert();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static AffineTransform inverted(AffineTransform transform) {
		transform = copy(transform);
		invert(transform);
		return transform;
	}

	public static Point2D point(double x, double y) {
		return new Point2D.Double(x, y);
	}

	public static Rectangle2D rectangle(double centerX, double centerY, double extentX, double extentY) {
		return new Rectangle2D.Double(centerX - extentX, centerY - extentY, 2 * extentX, 2 * extentY);
	}

	public static void translate(AffineTransform transform, Point2D point) {
		transform.translate(point.getX(), point.getY());
	}

	public static Rectangle2D translated(Rectangle2D rectangle, Point2D point) {
		return new Rectangle2D.Double(rectangle.getX() + point.getX(), rectangle.getY() + point.getY(), rectangle.getWidth(), rectangle.getHeight());
	}

	public static AffineTransform translated(AffineTransform transform, Point2D point) {
		AffineTransform result = (AffineTransform)transform.clone();
		translate(result, point);
		return result;
	}

}
