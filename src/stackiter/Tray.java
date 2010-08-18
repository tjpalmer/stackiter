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

	private double pad = 0.2;

	private Random random;

	private boolean actionConsumed;

	public Tray(int count) {
		blocks = new ArrayList<Block>();
		random = new Random();
		fill(count);
	}

	private void fill(int count) {
		while (blocks.size() < count) {
			blocks.add(randomBlock());
		}
	}

	private void flush() {
		int count = blocks.size();
		blocks.clear();
		fill(count);
	}

	public boolean isActionConsumed() {
		return actionConsumed;
	}

	public Block graspedBlock(Point2D point) {
		// First check to make sure we're in the range of the tray.
		actionConsumed = false;
		// TODO The width constant again!
		if (point.getX() < anchor.getX() + 5 + 2*pad) {
			// Check flusher first, actually.
			// TODO Consider making the flusher a first class object.
			if (0 < point.getY()  && point.getY() < flusherHeight + pad) {
				actionConsumed = true;
				flush();
			} else {
				// Now check the blocks if we didn't click the flusher.
				for (Block block: blocks) {
					if (block.contains(point)) {
						actionConsumed = true;
						blocks.remove(block);
						fill(blocks.size() + 1);
						return block;
					}
				}
			}
		}
		return null;
	}

	public void paint(Graphics2D graphics, AffineTransform transform) {
		Point2D position = point(anchor.getX(), anchor.getY() + flusherHeight + pad);
		for (Block block: blocks) {
			// Position and paint the block.
			Point2D extent = block.getExtent();
			block.setPosition(position.getX() + extent.getX() + pad, position.getY() + extent.getY() + pad);
			block.paint(graphics, transform);
			// Move up the line.
			position.setLocation(position.getX(), position.getY() + 2*extent.getY() + pad);
		}
		paintFlusher(graphics, transform);
	}

	private void paintFlusher(Graphics2D graphics, AffineTransform transform) {
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

}
