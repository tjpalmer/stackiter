package stackiter.sim;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * The buffet tray where new blocks appear for use.
 */
public class Tray {

	private boolean actionConsumed;

	private Point2D anchor;

	private List<Block> blocks;

	private Rectangle2D die;

	private Rectangle2D die2;

	private boolean fixedToDisplay;

	private double flusherHeight = 2;

	private double height;

	private Logger logger;

	private Point2D minBlockExtent = point(0.5, 0.5);

	private Point2D maxBlockExtent = point(5, 5);

	private double pad = 0.2;

	private Random random;

	private boolean rotateBlocks;

	public Tray() {
		blocks = new ArrayList<Block>();
		random = new Random();
	}

	public void fill() {
		logger.atomic(new Runnable() { @Override public void run() {
			// Log itself to make sure we're up to date.
			logger.logTray(Tray.this);
			// Start either flusherHeight above the bottom of the display, or at ground level.
			double startY = fixedToDisplay ? flusherHeight + pad : 0;
			Point2D position = point(0, startY);
			for (int b = 0; position.getY() < height || b < blocks.size(); b++) {
				// Position and paint the block.
				Block block;
				if (b < blocks.size()) {
					block = blocks.get(b);
				} else {
					block = randomBlock();
					blocks.add(block);
				}
				block.setPosition(0, 0);
				Rectangle2D bounds = applied(block.getTransform(), block.getBounds());
				block.setPosition(position.getX() + bounds.getMaxX() + pad, position.getY() + bounds.getMaxY() + pad);
				// Log the block.
				// TODO Log reference frame for tray blocks!!!
				logger.logItem(block);
				// Move up the line.
				position.setLocation(position.getX(), position.getY() + bounds.getHeight() + pad);
			}
		}});
		// Update flusher display while we are at it.
		double trayWidth = getWidth();
		double dieExtent = Math.min(0.75 * flusherHeight / 2, trayWidth / 5);
		die = rectangle(
			(trayWidth - 3*dieExtent) / 2 + dieExtent,
			flusherHeight + pad - dieExtent,
			dieExtent,
			dieExtent
		);
		die2 = rectangle(
			die.getCenterX() + dieExtent,
			pad + dieExtent,
			dieExtent,
			dieExtent
		);
		if (!fixedToDisplay) {
			// Drop it into the ground.
			double drop = flusherHeight + 2*pad;
			die.setRect(die.getX(), die.getY() - drop, die.getWidth(), die.getHeight());
			die2.setRect(die2.getX(), die2.getY() - drop, die2.getWidth(), die2.getHeight());
		}
	}

	public void flush() {
		logger.atomic(new Runnable() { @Override public void run() {
			for (Block block: blocks) {
				logger.logRemoval(block);
			}
		}});
		blocks.clear();
		fill();
	}

	public Point2D getAnchor() {
		return copy(anchor);
	}

	public Iterable<Block> getItems() {
		return blocks;
	}

	public Point2D getMaxBlockExtent() {
		return maxBlockExtent;
	}

	public Point2D getMinBlockExtent() {
		return minBlockExtent;
	}

	public Random getRandom() {
		return random;
	}

	public double getWidth() {
		return 2 * ((rotateBlocks ? norm(maxBlockExtent) : maxBlockExtent.getX()) + pad);
	}

	public Block graspedBlock(Point2D point) {
		// First check to make sure we're in the range of the tray.
		point = point(point.getX() - anchor.getX(), point.getY() - anchor.getY());
		actionConsumed = false;
		if (point.getX() < getWidth()) {
			// Check flusher first, actually.
			// TODO Consider making the flusher a first class object.
			if (die.contains(point) || die2.contains(point)) {
				actionConsumed = true;
				flush();
			} else {
				// Now check the blocks if we didn't click the flusher.
				for (Block block: blocks) {
					if (block.contains(point)) {
						actionConsumed = true;
						blocks.remove(block);
						Point2D blockPosition = block.getPosition();
						block.setPosition(blockPosition.getX() + anchor.getX(), blockPosition.getY() + anchor.getY());
						fill();
						return block;
					}
				}
			}
		}
		return null;
	}

	public boolean isActionConsumed() {
		return actionConsumed;
	}

	public boolean isFixedToDisplay() {
		return fixedToDisplay;
	}

	public boolean isRotateBlocks() {
		return rotateBlocks;
	}

	public void paint(Graphics2D graphics) {
		Graphics2D g = copy(graphics);
		try {
			g.translate(anchor.getX(), anchor.getY());
			// Blocks.
			for (Block block: blocks) {
				block.paint(g);
			}
			// Flusher.
			paintFlusher(g);
		} finally {
			g.dispose();
		}
	}

	private void paintDie(Rectangle2D die, int number, Graphics2D graphics) {
		// Face.
		graphics.setColor(Color.WHITE);
		graphics.fill(die);
		graphics.setColor(Color.BLACK);
		graphics.setStroke(new BasicStroke(0.2f));
		graphics.draw(die);
		// Dots.
		boolean[][][] dotsAll = {
			{{false, false, false}, {false, true, false}, {false, false, false}},
			{{true, false, false}, {false, false, false}, {false, false, true}},
			{{true, false, false}, {false, true, false}, {false, false, true}},
			{{true, false, true}, {false, false, false}, {true, false, true}},
			{{true, false, true}, {false, true, false}, {true, false, true}},
			{{true, false, true}, {true, false, true}, {true, false, true}},
		};
		boolean[][] dots = dotsAll[number - 1];
		double dotSpacing = die.getWidth() / 4;
		double dotSize = die.getWidth() / 5;
		double dotOffset = dotSpacing - dotSize/2;
		for (int j = 0; j < 3; j++) {
			double x = dotSpacing*j + dotOffset;
			for (int i = 0; i < 3; i++) {
				double y = dotSpacing*i + dotOffset;
				if (dots[i][j]) {
					Ellipse2D dot = new Ellipse2D.Double(die.getX() + x, die.getY() + y, dotSize, dotSize);
					graphics.fill(dot);
				}
			}
		}
	}

	private void paintFlusher(Graphics2D graphics) {
		paintDie(die, 5, graphics);
		paintDie(die2, 6, graphics);
	}

	public Block randomBlock() {
		Block block = new Block();
		block.setColor(randomColor());
		// TODO Consider Gaussian mixture model on sizes.
		// TODO Extract min and max sizes for less hardcoding on decorations and flusher.
		Point2D spread = subtracted(maxBlockExtent, minBlockExtent);
		block.setExtent(
			spread.getX() * random.nextDouble() + minBlockExtent.getX(),
			spread.getY() * random.nextDouble() + minBlockExtent.getY()
		);
		if (rotateBlocks) {
			block.setRotation(2 * random.nextDouble() - 1);
		}
		return block;
	}

	public Color randomColor() {
		// TODO Consider Gaussian mixture model on saturations and brightnesses. Hue should likely be uniform, though, I think.
		return Color.getHSBColor(random.nextFloat(), random.nextFloat(), random.nextFloat());
	}

	public void setAnchor(Point2D anchor) {
		this.anchor = anchor;
	}

	public void setFixedToDisplay(boolean fixedToDisplay) {
		this.fixedToDisplay = fixedToDisplay;
	}

	public void setHeight(double height) {
		if (this.height != height) {
			this.height = height;
			// We might need more blocks now.
			fill();
		}
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public void setMaxBlockExtent(Point2D maxBlockExtent) {
		this.maxBlockExtent = maxBlockExtent;
	}

	public void setMinBlockExtent(Point2D minBlockExtent) {
		this.minBlockExtent = minBlockExtent;
	}

	public void setRotateBlocks(boolean rotateBlocks) {
		this.rotateBlocks = rotateBlocks;
	}

}
