package stackiter;

import java.awt.*;
import java.awt.Shape;
import java.awt.geom.*;

import org.jbox2d.collision.*;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.*;

public class Block {

	private Body body;

	private BodyDef bodyDef;

	private Color color;

	private Point2D otherGraspPointOffset;

	private PolygonDef shapeDef;

	public Block() {
		// Placeholders.
		bodyDef = new BodyDef();
		shapeDef = new PolygonDef();
		// Some default values.
		shapeDef.setAsBox(1, 1);
		shapeDef.density = 4f;
		shapeDef.restitution = 0.0f;
		shapeDef.friction = 0.7f;
	}

	private void addDragJoint(Point2D point, double forceScale) {
		MouseJointDef jointDef = new MouseJointDef();
		jointDef.body1 = body.getWorld().getGroundBody();
		jointDef.body2 = body;
		jointDef.target.set((float)point.getX(), (float)point.getY());
		jointDef.maxForce = (float)(forceScale);
		body.getWorld().createJoint(jointDef);
	}

	public void addTo(World world) {
		body = world.createBody(bodyDef);
		body.createShape(shapeDef);
		body.setMassFromShapes();
	}

	public boolean contains(Point2D point) {
		Shape shape = transformedShape();
		return shape.contains(point);
	}

	private Rectangle2D getBounds() {
		XForm xForm = new XForm();
		xForm.setIdentity();
		AABB aabb = new AABB();
		body.getShapeList().computeAABB(aabb, xForm);
		double width = aabb.upperBound.x - aabb.lowerBound.x;
		double height = aabb.upperBound.y - aabb.lowerBound.y;
		Rectangle2D rectangle = new Rectangle2D.Double();
		rectangle.setRect(-width / 2, -height / 2, width, height);
		return rectangle;
	}

	public Color getColor() {
		return color;
	}

	private AffineTransform getTransform() {
		return getTransform(null);
	}

	private AffineTransform getTransform(AffineTransform transform) {
		if (transform == null) {
			transform = new AffineTransform();
		} else {
			transform.setToIdentity();
		}
		worldToBlockTransform(transform);
		return transform;
	}

	public void grasp(Point2D point) {
		//		try {
		//			AffineTransform transform = getTransform();
		//			Point2D blockPoint = transform.inverseTransform(point, null);
		//			// TODO First figure out whether X or Y relates to X in the global frame.
		//			blockPoint.setLocation(-blockPoint.getX(), blockPoint.getY());
		//			Point2D otherPoint = transform.transform(blockPoint, null);
		//			return otherPoint;
		//		} catch (Exception e) {
		//			throw new RuntimeException(e);
		//		}
		// TODO Put a center point with high force and side points with lower.
		addDragJoint(point, 500);
		Point2D otherPoint = otherGraspPoint(point);
		addDragJoint(otherPoint, 400);
		otherGraspPointOffset = new Point2D.Double(-otherPoint.getX() + point.getX(), -otherPoint.getY() + point.getY());
		body.wakeUp();
	}

	private double inverseTransformedWidth(AffineTransform transform, double width) {
		try {
			// TODO Look for an easier way.
			double[] out = new double[4];
			transform.inverseTransform(new double[] {0, 0, width, 0}, 0, out, 0, 2);
			double dX = out[2] - out[0];
			double dY = out[3] - out[1];
			return Math.sqrt(dX * dX + dY * dY);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Only meaningful if previously grasped?
	 */
	public void moveTo(Point2D point) {
		// Point 1.
		MouseJoint joint = (MouseJoint)body.getJointList().joint;
		joint.setTarget(new Vec2((float)point.getX(), (float)point.getY()));
		// Point 2.
		Point2D otherPoint = new Point2D.Double(point.getX() + otherGraspPointOffset.getX(), point.getY() + otherGraspPointOffset.getY());
		joint = (MouseJoint)body.getJointList().next.joint;
		joint.setTarget(new Vec2((float)otherPoint.getX(), (float)otherPoint.getY()));
	}

	private Point2D otherGraspPoint(Point2D point) {
		try {
			AffineTransform transform = getTransform();
			Point2D blockPoint = transform.inverseTransform(point, null);
			// TODO First figure out whether X or Y relates to X in the global frame.
			blockPoint.setLocation(-blockPoint.getX(), blockPoint.getY());
			Point2D otherPoint = transform.transform(blockPoint, null);
			return otherPoint;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void paint(Graphics2D graphics, AffineTransform transform) {
		Graphics2D g = (Graphics2D)graphics.create();
		try {
			// Shape information.
			double strokeWidth = 2;
			XForm xForm = body.getXForm();
			boolean anyRotation = !(xForm.R.col1.y == 0 && xForm.R.col2.x == 0);
			Shape shape = transformedShape(transform, strokeWidth);
			// Draw the block.
			g.setColor(color);
			g.fill(shape);
			g.setColor(color.darker());
			g.setStroke(new BasicStroke((float)strokeWidth));
			if (anyRotation) {
				// Seems a bit too slow to do antialiasing. Even in this specific context.
				// g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
			g.draw(shape);
		} finally {
			g.dispose();
		}
	}

	public void release() {
		while (body.getJointList() != null) {
			body.getWorld().destroyJoint(body.getJointList().joint);
		}
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setDensity(double density) {
		shapeDef.density = (float)density;
	}

	public void setExtent(double extentX, double extentY) {
		shapeDef.setAsBox((float)extentX, (float)extentY);
	}

	public void setPosition(double x, double y) {
		bodyDef.position = new Vec2((float)x, (float)y);
	}

	public void setRotation(double rotation) {
		bodyDef.angle = (float)(rotation * Math.PI);
	}

	public Shape transformedShape() {
		return transformedShape(new AffineTransform(), 0);
	}

	/**
	 * @param transform from display to block. Will be modified!
	 * @param inset in the display frame.
	 */
	private Shape transformedShape(AffineTransform transform, double inset) {
		worldToBlockTransform(transform);
		// Size, including inset amount.
		inset = inverseTransformedWidth(transform, inset);
		Rectangle2D rectangle = getBounds();
		rectangle.setRect(
			rectangle.getX() + inset/2, rectangle.getY() + inset/2,
			rectangle.getWidth() - inset, rectangle.getHeight() - inset
		);
		// Transformed rectangle.
		GeneralPath path = new GeneralPath(rectangle);
		Shape shape = path.createTransformedShape(transform);
		return shape;
	}

	private void worldToBlockTransform(AffineTransform transform) {
		// Position.
		XForm xForm = body.getXForm();
		transform.translate(xForm.position.x, xForm.position.y);
		// Rotation.
		transform.concatenate(new AffineTransform(new double[] {xForm.R.col1.x, xForm.R.col1.y, xForm.R.col2.x, xForm.R.col2.y}));
	}

}
