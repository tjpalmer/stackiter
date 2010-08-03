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

	private PolygonDef shapeDef;

	public Block() {
		// Placeholders.
		bodyDef = new BodyDef();
		shapeDef = new PolygonDef();
		// Some default values.
		shapeDef.setAsBox(1, 1);
		shapeDef.density = 1f;
		shapeDef.restitution = 0.0f;
		shapeDef.friction = 0.5f;
	}

	public void addJoint(Block other, Point2D point) {
		try {
			Point2D point1 = getTransform().inverseTransform(point, null);
			Point2D point2 = other.getTransform().inverseTransform(point, null);
			RevoluteJointDef jointDef = new RevoluteJointDef();
			jointDef.body1 = body;
			jointDef.body2 = other.body;
			jointDef.localAnchor1.x = (float)point1.getX();
			jointDef.localAnchor1.y = (float)point1.getY();
			jointDef.localAnchor2.x = (float)point2.getX();
			jointDef.localAnchor2.y = (float)point2.getY();
			body.getWorld().createJoint(jointDef);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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

	public void removeJoints() {
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
		// Size, including stroke size.
		XForm xForm = new XForm();
		xForm.setIdentity();
		AABB bounds = new AABB();
		body.getShapeList().computeAABB(bounds, xForm);
		inset = inverseTransformedWidth(transform, inset);
		double width = bounds.upperBound.x - bounds.lowerBound.x - inset;
		double height = bounds.upperBound.y - bounds.lowerBound.y - inset;
		// Transformed rectangle.
		GeneralPath path = new GeneralPath(new Rectangle2D.Double(-width / 2, -height / 2, width, height));
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
