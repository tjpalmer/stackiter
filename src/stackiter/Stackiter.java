package stackiter;

import static stackiter.Util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;

import org.jbox2d.collision.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;

@SuppressWarnings("serial")
public class Stackiter extends JComponent implements ActionListener {

	public static void main(String[] args) {
		JFrame frame = new JFrame("Stackiter");
		frame.setLayout(new BorderLayout());
		Stackiter stackiter = new Stackiter();
		frame.add(stackiter, BorderLayout.CENTER);
		frame.pack();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.addNotify();
		frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		stackiter.start();
	}

	private List<Block> blocks;

	private Block ground;

	private Block heldBlock;

	private Timer timer;

	private Tray tray;

	private Rectangle2D viewRect;

	private World world;

	public Stackiter() {
		setPreferredSize(new Dimension(600, 400));
		timer = new Timer(10, this);
		tray = new Tray(10);
		viewRect = new Rectangle2D.Double(-20, -5, 40, 25);
		MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent event) {
				Stackiter.this.mouseDragged(event);
			}
			@Override
			public void mousePressed(MouseEvent event) {
				Stackiter.this.mousePressed(event);
			}
			@Override
			public void mouseReleased(MouseEvent event) {
				Stackiter.this.mouseReleased(event);
			}
		};
		addMouseListener(mouseAdapter);
		addMouseMotionListener(mouseAdapter);
		// TODO Move out domain from display.
		blocks = new ArrayList<Block>();
		world = new World(new AABB(new Vec2(-100,-100), new Vec2(100,100)), new Vec2(0, -10), true);
		addGround();
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		// TODO Offload this to a separate thread? If so, still lock step to one update per frame.
		// TODO Alternatively, change the delay based on how much time is left.
		world.step(0.02f, 10);
		repaint();
	}

	private void addBlock(double y) {
		Block block = new Block();
		block.setColor(Color.getHSBColor(2/3f, 0.7f, 1f));
		block.setExtent(1, 1);
		block.setPosition(0, y);
		block.setRotation(2 * Math.random() - 1);
		block.addTo(world);
		blocks.add(block);
	}

	private void addGround() {
		ground = new Block();
		ground.setColor(Color.getHSBColor(1/12f, 0.5f, 0.5f));
		ground.setDensity(0);
		ground.setExtent(world.getWorldAABB().upperBound.x, 5);
		ground.setPosition(0, -5);
		ground.addTo(world);
	}

	protected void mouseDragged(MouseEvent event) {
		try {
			if (heldBlock != null) {
				Point2D point = new Point2D.Double();
				AffineTransform transform = worldToDisplayTransform();
				transform.inverseTransform(new Point2D.Double(event.getX(), event.getY()), point);
				heldBlock.moveTo(point);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void mousePressed(MouseEvent event) {
		try {
			Point2D point = new Point2D.Double();
			AffineTransform transform = worldToDisplayTransform();
			transform.inverseTransform(new Point2D.Double(event.getX(), event.getY()), point);
			for (Block block: blocks) {
				if (block.contains(point)) {
					heldBlock = block;
					// Don't break from loop. Make the last drawn have priority for clicking.
					// That's more intuitive when blocks overlap.
					// But how often will that be when physics tries to avoid it?
				}
			}
			if (heldBlock == null) {
				// No live blocks. Try reserve blocks.
				heldBlock = tray.graspedBlock(point);
				if (heldBlock != null) {
					blocks.add(heldBlock);
					heldBlock.addTo(world);
				}
			}
			if (heldBlock != null) {
				heldBlock.grasp(point);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void mouseReleased(MouseEvent event) {
		if (heldBlock != null) {
			heldBlock.release();
			heldBlock = null;
		}
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics.create();
		try {
			// Background.
			g.setColor(Color.WHITE);
			g.fill(getBounds());
			AffineTransform transform = worldToDisplayTransform();
			// Tray.
			// TODO Update tray bounds on window resize. Not here!
			updateTrayBounds();
			tray.paint(g, copy(transform));
			// Ground.
			ground.paint(g, copy(transform));
			// Live blocks.
			for (Block block: blocks) {
				block.paint(g, copy(transform));
			}
		} finally {
			g.dispose();
		}
	}

	private void start() {
		timer.start();
	}

	private void updateTrayBounds() {
		AffineTransform transform = worldToDisplayTransform();
		invert(transform);
		Point2D anchor = apply(transform, point(0, 0));
		anchor.setLocation(anchor.getX(), 0);
		tray.setAnchor(anchor);
	}

	private double worldToDisplayScale() {
		Dimension size = getSize();
		double xScale = size.getWidth() / viewRect.getWidth();
		double yScale = size.getHeight() / viewRect.getHeight();
		double scale = xScale * viewRect.getHeight() > size.getHeight() ? yScale : xScale;
		return scale;
	}

	private AffineTransform worldToDisplayTransform() {
		AffineTransform transform = new AffineTransform();
		transform.translate(0.5 * getWidth(), 0.5 * getHeight());
		double scale = worldToDisplayScale();
		transform.scale(scale, -scale);
		transform.translate(-viewRect.getCenterX(), -viewRect.getCenterY());
		return transform;
	}

}
