package stackiter;

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

	public static Point2D copy(Point2D point) {
		return (Point2D)point.clone();
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
