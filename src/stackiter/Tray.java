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
			if (anchor.getY() < point.getY() && point.getY() < + flusherHeight + 2*pad) {
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

		// Background for readability and to clarify click priority.
		// TODO It's a bit too slow for me when translucent. Frame period drops from about 10 to about 15 ms.
		// TODO Consider blitting or generally reduced frame rate?
		// TODO Or possibly make a font outline, but that doesn't clarify the click trump.
		// TODO   Something like this? new Font(null).createGlyphVector(null, "").getGlyphOutline(0);
		// TODO   Or create an image then dilate it by other means?
		graphics.setPaint(Color.WHITE);
		Rectangle2D backdrop = new Rectangle2D.Double(anchor.getX(), anchor.getY(), getWidth(), flusherHeight + pad);
		backdrop = new Path2D.Double(backdrop, transform).getBounds();
		graphics.fill(backdrop);

		// TODO Base width on max block size, once we have that constant.
		// TODO Improve centering also with font metrics.
		// TODO Standardize buttons and so on.
		transform = copy(transform);
		transform.translate(anchor.getX() + pad, anchor.getY() + 2*pad);
		transform.scale(1, -1);
		graphics.setFont(new Font(Font.SANS_SERIF, 0, (int)flusherHeight));
		graphics.transform(transform);
		graphics.setColor(Color.BLACK);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.drawString("Flush", 0, 0);

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
