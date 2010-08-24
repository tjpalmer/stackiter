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

	public enum PressAction {
		NONE, PRESS, RELEASE
	}

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

	private boolean mouseOver;

	private Point2D mousePoint = new Point2D.Double();

	private Point2D toolPoint = new Point2D.Double();

	private PressAction pressAction = PressAction.NONE;

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
		timer = new Timer(10, this);
		tray = new Tray();
		tray.setLogger(logger);
		viewBounds = new Rectangle2D.Double(-20, -3, 40, 101);
		viewRect = new Rectangle2D.Double(-20, -3, 40, 30);
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
		world = new World(new AABB(new Vec2(-100,-100), new Vec2(100,150)), new Vec2(0, -10), true);
		addGround();
		logger.logItem(ground);
		setSize(getPreferredSize());
	}

	@Override
	public void actionPerformed(ActionEvent event) {

		logger.atomic(new Runnable() { @Override public void run() {

			// Recalculate each time for cases of scrolling.
			// TODO Base instead on moving toolPoint explicitly when scrolling??? Could be risky.
			toolPoint = appliedInv(worldToDisplayTransform(), mousePoint);

			// Check release before moving grasped block.
			if (pressAction == PressAction.RELEASE) {
				handleRelease();
			}

			// Move the grasped block then scroll if needed.
			// TODO This is a chicken and egg problem here.
			if (graspedBlock != null) {
				graspedBlock.moveTo(toolPoint);
			}

			// But check grasps after attempting to move any grasped block.
			if (pressAction == PressAction.PRESS) {
				handlePress();
			}

			// And reset the press action now.
			pressAction = PressAction.NONE;

			if (mouseOver) {
				// Without mouseOver check, I got upward scrolling when over title bar.
				handleScroll(toolPoint);
			}
			updateView();

			// TODO Offload this to a separate thread? If so, still lock step to one update per frame.
			// TODO Alternatively, change the delay based on how much time is left.
			// Step the simulation.
			world.step(0.02f, 10);

			if (mouseOver) {
				// Make sure we get the entrance before the move, if both.
				logger.logToolPresent(mouseOver);
				logger.logMove(toolPoint);
			} else {
				// Make we get the move before the departure, if both.
				logger.logMove(toolPoint);
				logger.logToolPresent(mouseOver);
			}

			logger.logDisplaySize(point(getWidth(), getHeight()));
			logger.logView(viewRelWorld());

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
			logger.logItem(ground);
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

	private void handlePress() {
		// Try reserve blocks.
		graspedBlock = tray.graspedBlock(toolPoint);
		if (graspedBlock != null) {
			blocks.add(graspedBlock);
			graspedBlock.addTo(world);
		}
		if (!tray.isActionConsumed()) {
			for (Block block: blocks) {
				if (block.contains(toolPoint)) {
					graspedBlock = block;
					// Don't break from loop. Make the last drawn have priority for clicking.
					// That's more intuitive when blocks overlap.
					// But how often will that be when physics tries to avoid it?
				}
			}
		}
		if (graspedBlock != null) {
			// No blocks from tray. Try live blocks.
			graspedBlock.grasp(toolPoint);
			Point2D pointRelBlock = appliedInv(graspedBlock.getTransform(), toolPoint);
			logger.logGrasp(graspedBlock, pointRelBlock);
		}
	}

	private void handleRelease() {
		// TODO Log mouse releases independently from grasps. The grasps are cheating.
		if (graspedBlock != null) {
			logger.logRelease(graspedBlock);
			graspedBlock.release();
			graspedBlock = null;
		}
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
		mouseOver = true;
	}

	@Override
	public void mouseExited(MouseEvent event) {
		mouseOver = false;
	}

	@Override
	public void mouseMoved(MouseEvent event) {
		mousePoint.setLocation(event.getX(), event.getY());
		mouseOver = true;
	}

	@Override
	public void mousePressed(final MouseEvent event) {
		// Track latest mouse position.
		mouseMoved(event);
		// Defer action to world update.
		pressAction = PressAction.PRESS;
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		// Defer action to world update.
		// Possibly reduces responsiveness to user, but it keeps things more consistent from a standard MDP perspective.
		// And if the world is fast enough, things will be okay.
		pressAction = PressAction.RELEASE;
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = copy(graphics);
		try {
			// Transform.
			AffineTransform transform = worldToDisplayTransform();
			// Backdrop.
			Point2D backdropPoint = applied(transform, point(viewBounds.getMinX(), viewBounds.getMinY()));
			g.drawImage(backdrop, (int)backdropPoint.getX(), (int)backdropPoint.getY() - backdrop.getHeight(), null);
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
		int width = (int)Math.ceil(viewBounds.getWidth() * scale);
		int height = (int)Math.ceil(viewBounds.getHeight() * scale);
		if (backdrop == null || backdropScale != scale) {
			backdrop = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			backdropScale = scale;
			Graphics2D g = backdrop.createGraphics();
			try {
				float sectionHeight;
				// Black background (or can I assume this by default?).
				g.setColor(Color.BLACK);
				g.fill(getBounds());
				// Stars.
				// I've considered deferring stars to paint time, but this is actually fairly slow here.
				Random random = new Random(0);
				int starCount = (int)(0.001 * backdrop.getWidth() * backdrop.getHeight());
				for (int i = 0; i < starCount; i++) {
					if (i % (starCount / 20) == 0) {
						// The stars are already in random places.
						// Only change the brightness at intervals, for fewer objects and faster speed.
						g.setColor(Color.getHSBColor(1, 0, i / (float)starCount));
					}
					int x = random.nextInt(backdrop.getWidth());
					int y = random.nextInt(backdrop.getHeight());
					g.fillRect(x, y, 2, 2);
				}
				// Blue base to white.
				sectionHeight = (float)(20 * scale);
				float sectionY = backdrop.getHeight() - sectionHeight;
				g.setPaint(new GradientPaint(
					0, backdrop.getHeight(), Color.getHSBColor((float)(0.7 * 2/3f + 0.3 * 1/3f), 0.3f, 1f),
					0, sectionY, Color.WHITE
				));
				sectionY -= (float)(5 * scale);
				g.fillRect(0, (int)sectionY, backdrop.getWidth(), (int)(backdrop.getHeight() - sectionY));
				// White to transparent (i.e., black starry space).
				sectionHeight = (float)(30 * scale);
				float sectionY2 = sectionY - sectionHeight;
				g.setPaint(new GradientPaint(
					0, sectionY, Color.WHITE,
					0, sectionY2, new Color(0, 0, 0, 0)
				));
				g.fillRect(0, (int)sectionY2, backdrop.getWidth(), (int)sectionHeight);
			} finally {
				g.dispose();
			}
		}
	}

	private void updateTrayBounds() {
		AffineTransform transform = worldToDisplayTransform();
		invert(transform);
		Point2D anchor = applied(transform, point(0, getHeight()));
		tray.setAnchor(anchor);
		tray.setHeight(viewRelWorld().getHeight());
	}

	private void updateView() {
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
