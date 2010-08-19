package stackiter;

import java.awt.*;
import java.awt.geom.*;

public class Util {

	public static Point2D apply(AffineTransform transform, Point2D point) {
		return transform.transform(point, null);
	}

	public static Point2D applyInv(AffineTransform transform, Point2D point) {
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

	public static void translate(AffineTransform transform, Point2D point) {
		transform.translate(point.getX(), point.getY());
	}

	public static AffineTransform translated(AffineTransform transform, Point2D point) {
		AffineTransform result = (AffineTransform)transform.clone();
		translate(result, point);
		return result;
	}

}
