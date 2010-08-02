package stackiter;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;

import org.jbox2d.collision.*;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;

@SuppressWarnings("serial")
public class Stackiter extends JComponent implements ActionListener {

	public static void main(String[] args) {
		JFrame frame = new JFrame("Stackiter");
		frame.setLayout(new BorderLayout());
		Stackiter stackiter = new Stackiter();
		stackiter.setPreferredSize(new Dimension(600, 400));
		frame.add(stackiter, BorderLayout.CENTER);
		frame.pack();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
		stackiter.start();
	}

	private Body block;

	private Body ground;

	private Timer timer;

	private Rectangle2D viewRect;

	private World world;

	public Stackiter() {
		timer = new Timer(10, this);
		viewRect = new Rectangle2D.Double(-20, -5, 40, 25);
		System.out.println(viewRect.getCenterY());
		world = new World(new AABB(new Vec2(-100,-100), new Vec2(100,100)), new Vec2(0, -10), true);
		putGround();
		putBlock();
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		// TODO Offload this to a separate thread? If so, still lock step to one update per frame.
		// TODO Alternatively, change the delay based on how much time is left.
		world.step(0.02f, 10);
		repaint();
	}

	private void paintBlock(Body block, Color color, Graphics2D graphics) {
		Graphics2D g = (Graphics2D)graphics.create();
		try {
			double strokeWidth = 0.2;
			AABB bounds = new AABB();
			XForm xForm = block.getXForm();
			// Position.
			Vec2 pos = block.getPosition();
			g.translate(pos.x, pos.y);
			// Rotation.
			g.transform(new AffineTransform(new double[] {xForm.R.col1.x, xForm.R.col1.y, xForm.R.col2.x, xForm.R.col2.y}));
			// Size.
			xForm.setIdentity();
			block.getShapeList().computeAABB(bounds, xForm);
			double width = bounds.upperBound.x - bounds.lowerBound.x - strokeWidth;
			double height = bounds.upperBound.y - bounds.lowerBound.y - strokeWidth;
			Rectangle2D.Double shape = new Rectangle2D.Double(-width / 2, -height / 2, width, height);
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

	@Override
	protected void paintComponent(Graphics graphics) {
		Dimension size = getSize();
		double xScale = size.getWidth() / viewRect.getWidth();
		double yScale = size.getHeight() / viewRect.getHeight();
		double scale = xScale * viewRect.getHeight() > size.getHeight() ? yScale : xScale;
		Graphics2D g = (Graphics2D)graphics.create();
		try {
			g.setColor(Color.WHITE);
			g.fill(getBounds());
			g.translate(0.5 * size.getWidth(), 0.5 * size.getHeight());
			g.scale(scale, -scale);
			g.translate(-viewRect.getCenterX(), -viewRect.getCenterY());
			paintBlock(ground, Color.getHSBColor(1/12f, 0.5f, 0.5f), g);
			paintBlock(block, Color.getHSBColor(2/3f, 0.7f, 1f), g);
		} finally {
			g.dispose();
		}
	}

	private void putBlock() {
		BodyDef blockDef = new BodyDef();
		blockDef.position = new Vec2(0, 10);
		blockDef.angle = (float)(0.2 * Math.PI);
		block = world.createBody(blockDef);
		PolygonDef blockShape = new PolygonDef();
		blockShape.setAsBox(1, 1);
		blockShape.density = 1f;
		blockShape.restitution = 0.0f;
		blockShape.friction = 0.5f;
		block.createShape(blockShape);
		block.setMassFromShapes();
	}

	private void putGround() {
		AABB worldBounds = world.getWorldAABB();
		BodyDef groundDef = new BodyDef();
		groundDef.position = new Vec2(0, -5f);
		ground = world.createBody(groundDef);
		PolygonDef groundShape = new PolygonDef();
		groundShape.setAsBox(worldBounds.upperBound.x, 5f);
		groundShape.restitution = 0.0f;
		groundShape.friction = 0.5f;
		ground.createShape(groundShape);
	}

	private void start() {
		timer.start();
	}

}
