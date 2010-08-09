package stackiter;

import static stackiter.Util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;

import org.jbox2d.collision.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;

@SuppressWarnings("serial")
public class Stackiter extends JComponent implements ActionListener, Closeable {

	public static void main(String[] args) {
		JFrame frame = new JFrame("Stackiter");
		frame.setLayout(new BorderLayout());
		final Stackiter stackiter = new Stackiter();
		frame.add(stackiter, BorderLayout.CENTER);
		frame.pack();
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				stackiter.close();
			}
		});
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.addNotify();
		frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		stackiter.start();
	}

	private List<Block> blocks;

	private Block ground;

	private Block heldBlock;

	private Logger logger;

	private Timer timer;

	private Tray tray;

	private Rectangle2D viewRect;

	private World world;

	public Stackiter() {
		setPreferredSize(new Dimension(600, 400));
		logger = new Logger();
		timer = new Timer(10, this);
		tray = new Tray(20);
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
		// Step the simulation.
		world.step(0.02f, 10);
		// Delete lost blocks.
		AffineTransform transform = inverted(worldToDisplayTransform());
		Path2D displayPath = new Path2D.Double(getBounds());
		displayPath.transform(transform);
		Rectangle2D displayBounds = displayPath.getBounds2D();
		for (Iterator<Block> b = blocks.iterator(); b.hasNext();) {
			Block block = b.next();
			Shape blockShape = block.transformedShape();
			Rectangle2D blockBounds = blockShape.getBounds2D();
			if (!(block == heldBlock || displayBounds.contains(blockBounds) || blockShape.intersects(displayBounds))) {
				b.remove();
				block.removeFromWorld();
			}
		}
		// Record the new state.
		for (Block block: blocks) {
			logger.logItem(block);
		}
		// And queue the repaint.
		repaint();
	}

	private void addGround() {
		ground = new Block();
		ground.setColor(Color.getHSBColor(0, 0, 0.5f));
		ground.setDensity(0);
		ground.setExtent(world.getWorldAABB().upperBound.x, 5);
		ground.setPosition(0, -5);
		ground.addTo(world);
	}

	public void close() {
		logger.close();
	}

	private void mouseDragged(MouseEvent event) {
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

	private void mousePressed(MouseEvent event) {
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

	private void mouseReleased(MouseEvent event) {
		if (heldBlock != null) {
			heldBlock.release();
			heldBlock = null;
		}
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = copy(graphics);
		try {
			// Background.
			g.setColor(Color.WHITE);
			g.fill(getBounds());
			AffineTransform transform = worldToDisplayTransform();
			// Ground.
			ground.paint(g, transform);
			// Tray.
			// TODO Update tray bounds on window resize. Not here!
			updateTrayBounds();
			tray.paint(g, transform);
			// Live blocks.
			for (Block block: blocks) {
				block.paint(g, transform);
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
		// Tray.
		invert(transform);
		Point2D anchor = apply(transform, point(0, 0));
		anchor.setLocation(anchor.getX(), 0);
		tray.setAnchor(anchor);
		Point2D bottom = apply(transform, point(0, getHeight()));
		tray.setBottom(bottom.getY());
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
