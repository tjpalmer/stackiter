package stackiter.sim;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import org.jbox2d.collision.*;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.*;

public class Block implements Cloneable, Item {

	private boolean alive;

	private Body body;

	private BodyDef bodyDef;

	private Color color;

	private boolean debugPaint;

	private List<Block> fixations = new ArrayList<Block>();

	private MouseJoint mainJoint;

	private PolygonDef shapeDef;

	public Block() {
		// Placeholders.
		bodyDef = new BodyDef();
		shapeDef = new PolygonDef();
		// Some default values.
		shapeDef.setAsBox(1, 1);
		shapeDef.density = 1;
		shapeDef.restitution = 0.25f;
		shapeDef.friction = 0.5f;
	}

	private MouseJoint addDragJoint(Point2D point, double forceScale) {
		MouseJointDef jointDef = new MouseJointDef();
		jointDef.body1 = body.getWorld().getGroundBody();
		jointDef.body2 = body;
		jointDef.dampingRatio = (float)(Math.pow(body.getMass(), 0.5) / 2);
		jointDef.frequencyHz = 2;
		jointDef.target.set((float)point.getX(), (float)point.getY());
		// Constant force, but 5 is _near_ the top range of our masses.
		jointDef.maxForce = (float)(forceScale * 5); // * body.getMass());
		return (MouseJoint)body.getWorld().createJoint(jointDef);
	}

	private void addFixConstraints(Block other) {
		if (body != null && other.body != null) {
			// TODO Verify the joints aren't already added.
			// TODO Then add the joints.
			body.getJointList();
		}
	}

	public void addTo(World world) {
		setAlive(true);
		body = world.getDynamicsWorld().createBody(bodyDef);
		body.createShape(shapeDef);
		body.setMassFromShapes();
		for (Block fixed: fixations) {
			addFixConstraints(fixed);
		}
	}

	public void affix(Block other) {
		fixations.add(other);
		other.fixations.add(this);
		addFixConstraints(other);
	}

	public boolean contains(Point2D point) {
		Shape shape = transformedShape();
		return shape.contains(point);
	}

	@Override
	public Block clone() {
		Block copied = new Block();
		copied.alive = alive;
		copied.color = color;
		copied.setAngle(getAngle());
		copied.setPosition(getPosition());
		copied.setExtent(getExtent());
		return copied;
	}

	/**
	 * @return angle in rats (radians / pi).
	 */
	@Override
	public double getAngle() {
		if (body == null) {
			return bodyDef.angle / Math.PI;
		} else {
			XForm xForm = body.getXForm();
			double angle = -Math.signum(xForm.R.col2.x) * Math.acos(xForm.R.col1.x);
			return angle / Math.PI;
		}
	}

	public Rectangle2D getBounds() {
		Point2D extent = getExtent();
		Rectangle2D rectangle = new Rectangle2D.Double(-extent.getX(), -extent.getY(), 2 * extent.getX(), 2 * extent.getY());
		return rectangle;
	}

	public Color getColor() {
		return color;
	}

	@Override
	public Point2D getExtent() {
		// TODO Just store our own shape info instead of this dual mess.
		double width;
		double height;
		if (body == null) {
			double minX = Double.POSITIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY;
			double maxY = Double.NEGATIVE_INFINITY;
			for (Vec2 point: shapeDef.getVertexArray()) {
				minX = Math.min(point.x, minX);
				minY = Math.min(point.y, minY);
				maxX = Math.max(point.x, maxX);
				maxY = Math.max(point.y, maxY);
			}
			width = maxX - minX;
			height = maxY - minY;
		} else {
			XForm xForm = new XForm();
			xForm.setIdentity();
			AABB aabb = new AABB();
			body.getShapeList().computeAABB(aabb, xForm);
			width = aabb.upperBound.x - aabb.lowerBound.x;
			height = aabb.upperBound.y - aabb.lowerBound.y;
		}
		return point(width / 2, height / 2);
	}

	@Override
	public Point2D getPosition() {
		Vec2 position;
		if (body == null) {
			position = bodyDef.position;
		} else {
			position = body.getXForm().position;
		}
		return point(position.x, position.y);
	}

	public AffineTransform getTransform() {
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
		// Assume horizontal grasps, so find if X or Y local axis is horizontal.
		// TODO Also bias for thinner sides? Best for crazy shapes would be to look nearby for closest opposing edges.
		AffineTransform transform = getTransform();
		// Grasp points in block coordinate frame.
		Point2D blockPoint = appliedInv(transform, point);
		Point2D blockPointMin = new Point2D.Double();
		Point2D blockPointMax = new Point2D.Double();
		// Find the wrapping points.
		Rectangle2D bounds = getBounds();
		if (bounds.getWidth() < bounds.getHeight()) {
			blockPointMin.setLocation(bounds.getMinX(), blockPoint.getY());
			blockPointMax.setLocation(bounds.getMaxX(), blockPoint.getY());
		} else {
			blockPointMin.setLocation(blockPoint.getX(), bounds.getMinY());
			blockPointMax.setLocation(blockPoint.getX(), bounds.getMaxY());
		}
		Point2D pointMin = applied(transform, blockPointMin);
		Point2D pointMax = applied(transform, blockPointMax);
		// Calculate forces. Less stable the further from the center.
		double force = 50;
		double distX = Math.abs(blockPoint.getX() / bounds.getWidth());
		double distY = Math.abs(blockPoint.getY() / bounds.getHeight());
		double dist = 2 * Math.max(distX, distY); // 2x because earlier div by diameter, not radius.
		double supportForce = 0.6 * force * Math.pow((1 - dist), 5);
		// Normalize total max force for gain control.
		double ratio = 100 / (force + 2 * supportForce);
		force *= ratio;
		supportForce *= ratio;
		// Add drag joints.
		mainJoint = addDragJoint(point, force);
		if (supportForce > 0.01) {
			addDragJoint(pointMin, supportForce);
			addDragJoint(pointMax, supportForce);
		}
		// Wake up the body. It's awake if grasped.
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

	public boolean isAlive() {
		return alive;
	}

	private boolean isRotated() {
		if (body == null) {
			return bodyDef.angle != 0;
		} else {
			XForm xForm = body.getXForm();
			return !(xForm.R.col1.y == 0 && xForm.R.col2.x == 0);
		}
	}

	/**
	 * Only meaningful if previously grasped?
	 */
	public void moveTo(Point2D point) {
		Vec2 mainTargetNew = new Vec2((float)point.getX(), (float)point.getY());
		Vec2 mainTargetOld = mainJoint.getAnchor1().clone();
		for (JointEdge j = body.getJointList(); j != null; j = j.next) {
			MouseJoint joint = (MouseJoint)j.joint;
			joint.getAnchor1().addLocal(mainTargetNew.sub(mainTargetOld));
		}
		body.wakeUp();
	}

	@Override
	public void paint(Graphics2D graphics, AffineTransform worldRelDisplay) {
		Graphics2D g = copy(graphics);
		try {
			// Shape information.
			double strokeWidth = 2;
			Shape shape = transformedShape(copy(worldRelDisplay), strokeWidth);
			// Draw the block.
			g.setColor(color);
			g.fill(shape);
			g.setColor(color.darker());
			g.setStroke(new BasicStroke((float)strokeWidth));
			if (isRotated()) {
				// Seems a bit too slow to do antialiasing. Even in this specific context.
				// g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
			g.draw(shape);
			if (debugPaint) {
				paintJoints(g, worldRelDisplay);
			}
		} finally {
			g.dispose();
		}
	}

	private void paintJoints(Graphics2D graphics, AffineTransform transform) {
		if (isAlive()) {
			graphics.setColor(Color.BLACK);
			int j = 0;
			JointEdge edge = body.getJointList();
			while (edge != null) {
				MouseJoint joint = (MouseJoint)edge.joint;
				Vec2 target = joint.getAnchor1();
				Point2D point = point(target.x, target.y);
				point = applied(transform, point);
				graphics.drawString(String.valueOf(j), (float)point.getX(), (float)point.getY());
				edge = edge.next;
				j++;
			}
		}
	}

	public void release() {
		while (body.getJointList() != null) {
			body.getWorld().destroyJoint(body.getJointList().joint);
		}
	}

	public void removeFromWorld() {
		setAlive(false);
		if (body != null) {
			// Copy over the transform to remember the live state.
			bodyDef.angle = (float)getAngle();
			bodyDef.position.x = (float)getPosition().getX();
			bodyDef.position.y = (float)getPosition().getY();
			// TODO Could copy over other info.
			// Now wipe out the body.
			body.getWorld().destroyBody(body);
			body = null;
		}
	}

	/**
	 * Manually control the alive setting rather than depending on connection to
	 * world.
	 */
	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	/**
	 * @param angle in rats (radians / pi).
	 */
	public void setAngle(double angle) {
		bodyDef.angle = (float)(angle * Math.PI);
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

	public void setExtent(Point2D extent) {
		setExtent(extent.getX(), extent.getY());
	}

	public void setPosition(double x, double y) {
		bodyDef.position = new Vec2((float)x, (float)y);
	}

	public void setPosition(Point2D position) {
		setPosition(position.getX(), position.getY());
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
		if (body == null) {
			transform.translate(bodyDef.position.x, bodyDef.position.y);
			transform.rotate(bodyDef.angle);
		} else {
			// Position.
			XForm xForm = body.getXForm();
			transform.translate(xForm.position.x, xForm.position.y);
			// Rotation.
			transform.concatenate(new AffineTransform(new double[] {xForm.R.col1.x, xForm.R.col1.y, xForm.R.col2.x, xForm.R.col2.y}));
		}
	}

}
