package stackiter;

import static stackiter.Util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * The buffet tray where new blocks appear for use.
 */
public class Tray {

	private Point2D anchor;

	private List<Block> blocks;

	private double flusherHeight = 2;

	private double height;

	private Logger logger;

	private double maxBlockExtent = 2.5;

	private double pad = 0.2;

	private Random random;

	private boolean actionConsumed;

	private Rectangle2D die;

	private Rectangle2D die2;

	public Tray() {
		blocks = new ArrayList<Block>();
		random = new Random();
	}

	private void fill() {
		logger.atomic(new Runnable() { @Override public void run() {
			Point2D position = point(0, flusherHeight + pad);
			for (int b = 0; position.getY() < height || b < blocks.size(); b++) {
				// Position and paint the block.
				Block block;
				if (b < blocks.size()) {
					block = blocks.get(b);
				} else {
					block = randomBlock();
					blocks.add(block);
				}
				Point2D extent = block.getExtent();
				block.setPosition(position.getX() + extent.getX() + pad, position.getY() + extent.getY() + pad);
				// Log the block.
				logger.logItem(block);
				// Move up the line.
				position.setLocation(position.getX(), position.getY() + 2*extent.getY() + pad);
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
	}

	private void flush() {
		logger.atomic(new Runnable() { @Override public void run() {
			for (Block block: blocks) {
				logger.logRemoval(block);
			}
		}});
		blocks.clear();
		fill();
	}

	public Point2D getAnchor() {
		return anchor;
	}

	public double getWidth() {
		return 2 * (maxBlockExtent + pad);
	}

	public Block graspedBlock(Point2D point) {
		// First check to make sure we're in the range of the tray.
		point = point(point.getX() - anchor.getX(), point.getY() - anchor.getY());
		actionConsumed = false;
		// TODO The width constant again!
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

	public void paint(Graphics2D graphics, AffineTransform transform) {
		// Blocks.
		AffineTransform blockTransform = copy(transform);
		blockTransform.translate(anchor.getX(), anchor.getY());
		for (Block block: blocks) {
			block.paint(graphics, blockTransform);
		}
		// Flusher.
		paintFlusher(graphics, transform);
	}

	private void paintFlusher(Graphics2D graphics, AffineTransform transform) {
		paintDie(translated(die, anchor), 5, graphics, transform);
		paintDie(translated(die2, anchor), 6, graphics, transform);
	}

	private void paintDie(Rectangle2D die, int number, Graphics2D graphics, AffineTransform transform) {
		// Face.
		Rectangle2D dieDisplay = applied(transform, die);
		graphics.setColor(Color.WHITE);
		graphics.fill(dieDisplay);
		graphics.setColor(Color.BLACK);
		graphics.setStroke(new BasicStroke(3));
		graphics.draw(dieDisplay);
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
					graphics.fill(applied(transform, dot));
				}
			}
		}
	}

	private Block randomBlock() {
		Block block = new Block();
		block.setColor(randomColor());
		// TODO Consider Gaussian mixture model on sizes.
		// TODO Extract min and max sizes for less hardcoding on decorations and flusher.
		block.setExtent(2 * random.nextDouble() + 0.5, 2 * random.nextDouble() + 0.5);
		return block;
	}

	private Color randomColor() {
		// TODO Consider Gaussian mixture model on saturations and brightnesses. Hue should likely be uniform, though, I think.
		return Color.getHSBColor(random.nextFloat(), random.nextFloat(), random.nextFloat());
	}

	public void setAnchor(Point2D anchor) {
		this.anchor = anchor;
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

}
