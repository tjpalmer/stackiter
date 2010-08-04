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

	private Point2D.Double graspOffsetMin;

	private Point2D.Double graspOffsetMax;

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

	private Point2D.Double addDragJoint(Point2D point, Point2D base, double forceScale) {
		MouseJointDef jointDef = new MouseJointDef();
		jointDef.body1 = body.getWorld().getGroundBody();
		jointDef.body2 = body;
		jointDef.target.set((float)point.getX(), (float)point.getY());
		jointDef.maxForce = (float)(forceScale);
		body.getWorld().createJoint(jointDef);
		return new Point2D.Double(point.getX() - base.getX(), point.getY() - base.getY());
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
		try {
			// Assume horizontal grasps, so find if X or Y local axis is horizontal.
			// TODO Also bias for thinner sides? Best for crazy shapes would be to look nearby for closest opposing edges.
			AffineTransform transform = getTransform();
			Point2D xInWorld = transform.transform(new Point2D.Double(1, 0), null);
			// Grasp points in block coordinate frame.
			Point2D blockPoint = transform.inverseTransform(point, null);
			Point2D blockPointMin = new Point2D.Double();
			Point2D blockPointMax = new Point2D.Double();
			// Find the wrapping points.
			// TODO Consider putting the main point in the middle rather than at the mouse position.
			Rectangle2D bounds = getBounds();
			boolean onX = Math.abs(xInWorld.getX()) > Math.abs(xInWorld.getY());
			if (onX) {
				blockPointMin.setLocation(bounds.getMinX(), blockPoint.getY());
				blockPointMax.setLocation(bounds.getMaxX(), blockPoint.getY());
			} else {
				blockPointMin.setLocation(blockPoint.getY(), bounds.getMinY());
				blockPointMax.setLocation(blockPoint.getY(), bounds.getMaxY());
			}
			Point2D pointMin = transform.transform(blockPointMin, null);
			Point2D pointMax = transform.transform(blockPointMax, null);
			// Add drag joints and remember offsets.
			addDragJoint(point, point, 500);
			graspOffsetMin = addDragJoint(pointMin, point, 300);
			graspOffsetMax = addDragJoint(pointMax, point, 300);
			// Wake up the body. It's alive if grasped.
			body.wakeUp();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
		// Middle point.
		MouseJoint joint = (MouseJoint)body.getJointList().joint;
		joint.setTarget(new Vec2((float)point.getX(), (float)point.getY()));
		// Min point.
		joint = (MouseJoint)body.getJointList().next.joint;
		point = new Point2D.Double(point.getX() + graspOffsetMin.getX(), point.getY() + graspOffsetMin.getY());
		joint.setTarget(new Vec2((float)point.getX(), (float)point.getY()));
		// Max point.
		joint = (MouseJoint)body.getJointList().next.next.joint;
		point = new Point2D.Double(point.getX() + graspOffsetMax.getX(), point.getY() + graspOffsetMax.getY());
		joint.setTarget(new Vec2((float)point.getX(), (float)point.getY()));
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
