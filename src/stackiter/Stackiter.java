package stackiter;

import static stackiter.Util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;

import org.jbox2d.collision.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;

@SuppressWarnings("serial")
public class Stackiter extends JComponent implements ActionListener, Closeable, MouseListener, MouseMotionListener {

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

	private BufferedImage backdrop;

	private double backdropScale;

	Rectangle2D blockBoundsAllButGrasped = new Rectangle2D.Double();

	private List<Block> blocks;

	private double edgeThickness = 2.5;

	private Block ground;

	private Block graspedBlock;

	private Logger logger;

	private Point2D mousePoint;

	private Timer timer;

	private Tray tray;

	/**
	 * The outer bounds of what we are allowed to see.
	 */
	private Rectangle2D viewBounds;

	/**
	 * The current view we'd like to see, if we had the right aspect ratio. The
	 * size of this rectangle shouldn't ever change.
	 */
	private Rectangle2D viewRect;

	private World world;

	public Stackiter() {
		setPreferredSize(new Dimension(640, 480));
		logger = new Logger();
		mousePoint = new Point2D.Double();
		timer = new Timer(10, this);
		tray = new Tray(20);
		viewBounds = new Rectangle2D.Double(-20, -1, 40, 30);
		viewRect = new Rectangle2D.Double(-20, -1, 40, 30);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				Stackiter.this.componentResized(event);
			}
		});
		addMouseListener(this);
		addMouseMotionListener(this);
		// TODO Move out domain from display.
		blocks = new ArrayList<Block>();
		world = new World(new AABB(new Vec2(-100,-100), new Vec2(100,100)), new Vec2(0, -10), true);
		addGround();
		setSize(getPreferredSize());
	}

	@Override
	public void actionPerformed(ActionEvent event) {

		// Move the grasped block then scroll if needed.
		// TODO This is a chicken and egg problem here.
		final Point2D point = applyInv(worldToDisplayTransform(), mousePoint);
		if (graspedBlock != null) {
			graspedBlock.moveTo(point);
		}
		handleScroll(point);
		updateView();

		// TODO Offload this to a separate thread? If so, still lock step to one update per frame.
		// TODO Alternatively, change the delay based on how much time is left.
		// Step the simulation.
		world.step(0.02f, 10);

		logger.atomic(new Runnable() { @Override public void run() {
			logger.logMove(point);
			// Delete lost blocks.
			blockBoundsAllButGrasped.setRect(0, 0, 0, 0);
			for (Iterator<Block> b = blocks.iterator(); b.hasNext();) {
				Block block = b.next();
				Shape blockShape = block.transformedShape();
				Rectangle2D blockBounds = blockShape.getBounds2D();
				if (blockBounds.getMaxY() < -5) {
					// It fell off the table. Out of sight, out of mind.
					b.remove();
					block.removeFromWorld();
					logger.logRemoval(block);
				} else {
					// Keep track of where the blocks reach.
					if (block != graspedBlock) {
						Rectangle2D.union(blockBoundsAllButGrasped, blockBounds, blockBoundsAllButGrasped);
					}
				}
			}
			// Record the new state.
			for (Block block: blocks) {
				logger.logItem(block);
			}
		}});

		// And queue the repaint.
		repaint();

	}

	private void addGround() {
		ground = new Block();
		ground.setColor(Color.getHSBColor(0, 0, 0.5f));
		ground.setDensity(0);
		ground.setExtent(viewRect.getWidth()/2 - 5.5, 5);
		ground.setPosition(0, -5);
		ground.addTo(world);
	}

	public void close() {
		logger.close();
	}

	public void componentResized(ComponentEvent event) {
		updateView();
	}

	private Point2D eventPointToWorld(MouseEvent event) {
		Point2D point = applyInv(worldToDisplayTransform(), point(event.getX(), event.getY()));
		return point;
	}

	private void handleScroll(Point2D toolPoint) {

		double rateX = 0;
		double rateY = 0;
		Rectangle2D viewRelWorld = viewRelWorld();

		if (graspedBlock == null && toolPoint.getX() < tray.getAnchor().getX() + tray.getWidth()) {
			// No block and over the tray means we are probably thinking about the tray, not scrolling.
			// TODO Generalize this notion for widgets.
			return;
		}

		// See how close we are to the edge.
		if (toolPoint.getX() < viewRelWorld.getMinX() + edgeThickness) {
			// Scroll left if space available.
			rateX = Math.min(viewRelWorld.getMinX() - toolPoint.getX(), 0);
		} else if (toolPoint.getX() > viewRelWorld.getMaxX() - edgeThickness) {
			// Scroll right if space available.
			rateX = Math.max(viewRelWorld.getMaxX() - toolPoint.getX(), 0);
		}
		if (toolPoint.getY() < viewRelWorld.getMinY() + edgeThickness) {
			// Scroll down if space available.
			rateY = Math.min(viewRelWorld.getMinY() - toolPoint.getY(), 0);
		} else if (toolPoint.getY() > viewRelWorld.getMaxY() - edgeThickness) {
			// Scroll up if space available.
			rateY = Math.max(viewRelWorld.getMaxY() - toolPoint.getY(), 0);
		}

		// Scale by edge thickness and max speed.
		// TODO Nonlinear?
		rateX = rateX / edgeThickness;
		rateY = rateY / edgeThickness;
		rateX = 0.5 * Math.pow(1 - Math.abs(rateX), 2) * Math.signum(rateX);
		rateY = 0.5 * Math.pow(1 - Math.abs(rateY), 2) * Math.signum(rateY);

		// Impose view bounds.
		// TODO Do narrow bounds drive this crazy?
		// TODO Vectorized math would sure be nice here. This repetition is silly.
		//System.out.println(viewBounds);
		if (viewRelWorld.getMaxX() + rateX > viewBounds.getMaxX()) {
			rateX = viewBounds.getMaxX() - viewRelWorld.getMaxX();
		}
		if (viewRelWorld.getMinX() + rateX < viewBounds.getMinX()) {
			rateX = viewBounds.getMinX() - viewRelWorld.getMinX();
		}
		if (viewRelWorld.getMaxY() + rateY > viewBounds.getMaxY()) {
			rateY = viewBounds.getMaxY() - viewRelWorld.getMaxY();
		}
		if (viewRelWorld.getMinY() + rateY < viewBounds.getMinY()) {
			rateY = viewBounds.getMinY() - viewRelWorld.getMinY();
		}

		// Disable horizontal scrolling for now. It complicates having widgets on the sides.
		// TODO Define the bounds apart from widgets and use that edge for scrolling? No. You don't want to scroll just to get to the widgets. And bringing in the widgets makes things congested.
		// TODO Maybe make the widgets actually part of the world sometime instead of floating?
		rateX = 0;

		// Apply scroll rate, leaving the size unchanged.
		viewRect.setRect(viewRect.getX() + rateX, viewRect.getY() + rateY, viewRect.getWidth(), viewRect.getHeight());

	}

	@Override
	public void mouseClicked(MouseEvent event) {
		// Nothing to do here.
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		mouseMoved(event);
	}

	@Override
	public void mouseEntered(MouseEvent event) {
		logger.logEnter();
	}

	@Override
	public void mouseExited(MouseEvent event) {
		logger.logLeave();
	}

	@Override
	public void mouseMoved(MouseEvent event) {
		mousePoint.setLocation(event.getX(), event.getY());
	}

	@Override
	public void mousePressed(final MouseEvent event) {
		Point2D point = eventPointToWorld(event);
		// No live blocks. Try reserve blocks.
		graspedBlock = tray.graspedBlock(point);
		if (graspedBlock != null) {
			blocks.add(graspedBlock);
			graspedBlock.addTo(world);
		}
		if (!tray.isActionConsumed()) {
			for (Block block: blocks) {
				if (block.contains(point)) {
					graspedBlock = block;
					// Don't break from loop. Make the last drawn have priority for clicking.
					// That's more intuitive when blocks overlap.
					// But how often will that be when physics tries to avoid it?
				}
			}
		}
		if (graspedBlock != null) {
			graspedBlock.grasp(point);
			Point2D pointRelBlock = applyInv(graspedBlock.getTransform(), point);
			logger.logGrasp(graspedBlock, pointRelBlock);
		}
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		if (graspedBlock != null) {
			logger.logRelease(graspedBlock);
			graspedBlock.release();
			graspedBlock = null;
		}
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = copy(graphics);
		try {
			// Transform.
			AffineTransform transform = worldToDisplayTransform();
			// Backdrop.
			g.drawImage(
				backdrop,
				0,
				(int)apply(transform, point(0, viewBounds.getMinY())).getY() - backdrop.getHeight(),
				null
			);
			// Ground.
			ground.paint(g, transform);
			// Live blocks.
			for (Block block: blocks) {
				block.paint(g, transform);
			}
			// Tray.
			tray.paint(g, transform);
		} finally {
			g.dispose();
		}
	}

	private void start() {
		timer.start();
	}

	private void updateBackdrop() {
		double scale = worldToDisplayScale();
		int width = getWidth();
		int height = (int)Math.ceil(viewBounds.getHeight() * scale);
		if (backdrop == null || backdropScale != scale || width != backdrop.getWidth() || height > backdrop.getHeight()) {
			// Make the backdrop twice as large as we've ever seen a need. Prevents constant reallocation.
			height = 2 * height;
			backdrop = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			backdropScale = scale;
			Graphics2D g = backdrop.createGraphics();
			try {
				g.setPaint(new GradientPaint(
					0, backdrop.getHeight(), Color.getHSBColor(2/3f, 0.3f, 1f),
					0, (float)(backdrop.getHeight() - 20*scale), Color.WHITE
				));
				g.fillRect(0, 0, backdrop.getWidth(), backdrop.getHeight());
			} finally {
				g.dispose();
			}
		}
	}

	private void updateTrayBounds() {
		AffineTransform transform = worldToDisplayTransform();
		// Tray.
		invert(transform);
		Point2D anchor = apply(transform, point(0, getHeight()));
		//anchor.setLocation(anchor.getX(), 0);
		tray.setAnchor(anchor);
	}

	private void updateView() {
		viewBounds.setFrameFromDiagonal(
			viewBounds.getMinX(), viewBounds.getMinY(),
			viewBounds.getMaxX(), Math.max(30, blockBoundsAllButGrasped.getMaxY() + 5)
		);
		updateBackdrop();
		updateTrayBounds();
	}

	private Rectangle2D viewRelWorld() {
		AffineTransform transform = inverted(worldToDisplayTransform());
		Path2D displayPath = new Path2D.Double(getBounds());
		displayPath.transform(transform);
		return displayPath.getBounds2D();
	}

	private double worldToDisplayScale() {
		Dimension size = getSize();
		double xScale = size.getWidth() / viewRect.getWidth();
		double yScale = size.getHeight() / viewRect.getHeight();
		// Go with the smaller of the two. Cut off rather than show extra.
		double scale = xScale * viewRect.getHeight() < size.getHeight() ? yScale : xScale;
		return scale;
	}

	private AffineTransform worldToDisplayTransform() {
		AffineTransform transform = new AffineTransform();
		transform.translate(0.5 * getWidth(), getHeight());
		double scale = worldToDisplayScale();
		transform.scale(scale, -scale);
		transform.translate(-viewRect.getCenterX(), -viewRect.getMinY());
		return transform;
	}

}
