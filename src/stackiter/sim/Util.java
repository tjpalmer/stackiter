package stackiter.sim;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class Util {

	public static double TWO_PI = 2 * Math.PI;

	public static Point2D added(Point2D a, Point2D b) {
		return point(a.getX() + b.getX(), a.getY() + b.getY());
	}

	/**
	 * Assumes no rotation.
	 */
	public static Ellipse2D applied(AffineTransform transform, Ellipse2D ellipse) {
		// TODO Handle this manually more efficiently?
		Path2D path = new Path2D.Double(ellipse.getBounds2D());
		path.transform(transform);
		ellipse = new Ellipse2D.Double();
		ellipse.setFrame(path.getBounds2D());
		return ellipse;
	}

	public static Point2D applied(AffineTransform transform, Point2D point) {
		return transform.transform(point, null);
	}

	/**
	 * Returns bounds after transformation, which aren't guaranteed to be tight.
	 * If no rotation, the output really is just a transformed version of the
	 * input.
	 */
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

	public static boolean approx(double a, double b, double epsilon) {
		return Math.abs(a - b) < epsilon;
	}

	public static boolean approx(Point2D a, Point2D b, double epsilon) {
		return a.distanceSq(b) < epsilon*epsilon;
	}

	public static boolean between(double x, double min, double max) {
		return min <= x && x <= max;
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

	@SuppressWarnings("hiding")
	public static <Item> boolean contains(Iterable<Item> items, Item item) {
		for (Item other: items) {
			// TODO Support nulls?
			if (item.equals(other)) {
				return true;
			}
		}
		return false;
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
	 * Whether or not the value changed to or from zero (within epsilon).
	 */
	public static boolean crossedZero(double current, double old, double epsilon) {
		int signNew = signApprox(current, epsilon);
		int signOld = signApprox(old, epsilon);
		return !(signNew == signOld);
	}

	/**
	 * Whether or not the x or y components changed to or from zero (within
	 * epsilon). This is dependent on world axes, but ground and gravity gives
	 * some excuse for the distinction.
	 */
	public static boolean crossedZero(Point2D current, Point2D old, double epsilon) {
		int xSignNew = signApprox(current.getX(), epsilon);
		int ySignNew = signApprox(current.getY(), epsilon);
		int xSignOld = signApprox(old.getX(), epsilon);
		int ySignOld = signApprox(old.getY(), epsilon);
		return !(xSignNew == xSignOld && ySignNew == ySignOld);
	}

	public static double dot(Point2D a, Point2D b) {
		return a.getX() * b.getX() + a.getY() * b.getY();
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

	@SuppressWarnings("hiding")
	public static <Item> List<Item> list(Iterable<Item> items) {
		ArrayList<Item> list = new ArrayList<Item>();
		for (Item item: items) {
			list.add(item);
		}
		return list;
	}

	/**
	 * Repeated draws a random Gaussian value until it is within the given
	 * limits, inclusive.
	 */
	public static double nextGaussianConstrained(
		Random random, double mean, double deviation, double min, double max
	) {
		if (mean < min || max < mean) {
			throw new RuntimeException(String.format(
				"Impossible constraints: %g outside [%g, %g]", mean, min, max
			));
		}
		while (true) {
			double x = random.nextGaussian() * deviation + mean;
			if (between(x, min, max)) {
				return x;
			}
		}
	}

	public static double norm(Point2D point) {
		return point.distance(0, 0);
	}

	public static Point2D point() {
		return new Point2D.Double();
	}

	public static Point2D point(double x, double y) {
		return new Point2D.Double(x, y);
	}

	public static Line2D segment(Point2D a, Point2D b) {
		return new Line2D.Double(a, b);
	}

	public static Line2D segment(double x1, double y1, double x2, double y2) {
		return new Line2D.Double(x1, y1, x2, y2);
	}

	public static int signApprox(double x, double epsilon) {
		return (int)(Math.abs(x) < epsilon ? 0 : Math.signum(x));
	}

	public static double randInRange(Random random, double min, double max) {
		return random.nextDouble() * (max - min) + min;
	}

	public static Rectangle2D rectangle() {
		return new Rectangle2D.Double();
	}

	public static Rectangle2D rectangle(Point2D center, Point2D extent) {
		return new Rectangle2D.Double(center.getX() - extent.getX(), center.getY() - extent.getY(), 2 * extent.getX(), 2 * extent.getY());
	}

	public static Rectangle2D rectangle(double centerX, double centerY, double extentX, double extentY) {
		return new Rectangle2D.Double(centerX - extentX, centerY - extentY, 2 * extentX, 2 * extentY);
	}

	public static Point2D scaled(double scale, Point2D point) {
		return point(scale * point.getX(), scale * point.getY());
	}

	public static Point2D subtracted(Point2D a, Point2D b) {
		return point(a.getX() - b.getX(), a.getY() - b.getY());
	}

	public static double subtractedAngle(double a, double b) {
		double diff = (a - b) % TWO_PI;
		if (diff >= Math.PI) {
			diff -= TWO_PI;
		} else if (diff < -Math.PI) {
			diff += TWO_PI;
		}
		return diff;
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
