package stackiter;

import java.awt.*;
import java.awt.Shape;
import java.awt.geom.*;

import org.jbox2d.collision.*;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;

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

	public void addTo(World world) {
		body = world.createBody(bodyDef);
		body.createShape(shapeDef);
		body.setMassFromShapes();
	}

	public Color getColor() {
		return color;
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
			double strokeWidth = 2;
			AABB bounds = new AABB();
			XForm xForm = body.getXForm();
			// Position.
			Vec2 pos = body.getPosition();
			transform.translate(pos.x, pos.y);
			// Rotation.
			transform.concatenate(new AffineTransform(new double[] {xForm.R.col1.x, xForm.R.col1.y, xForm.R.col2.x, xForm.R.col2.y}));
			// Size, including stroke size.
			xForm.setIdentity();
			body.getShapeList().computeAABB(bounds, xForm);
			double strokeInv = inverseTransformedWidth(transform, strokeWidth);
			double width = bounds.upperBound.x - bounds.lowerBound.x - strokeInv;
			double height = bounds.upperBound.y - bounds.lowerBound.y - strokeInv;
			// Transformed rectangle.
			GeneralPath path = new GeneralPath(new Rectangle2D.Double(-width / 2, -height / 2, width, height));
			Shape shape = path.createTransformedShape(transform);
			// Draw the block.
			g.setColor(color);
			g.fill(shape);
			g.setColor(color.darker());
			g.setStroke(new BasicStroke((float)strokeWidth));
			g.draw(shape);
		} finally {
			g.dispose();
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

}
