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

	private Random random;

	private double bottom;

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

	public Block graspedBlock(Point2D point) {
		// First check to make sure we're in the range of the tray.
		// TODO The width and pad constants again!
		if (point.getX() < anchor.getX() + 5 + 0.4) {
			// Check flusher first, actually.
			// TODO Consider making the flusher a first class object.
			if (point.getY() < 0) {
				flush();
			} else {
				// Now check the blocks if we didn't click the flusher.
				for (Block block: blocks) {
					if (block.contains(point)) {
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
		double pad = 0.2;
		Point2D position = copy(anchor);
		for (Block block: blocks) {
			// Position and paint the block.
			Point2D extent = block.getExtent();
			block.setPosition(position.getX() + extent.getX() + pad, position.getY() + extent.getY() + pad);
			block.paint(graphics, transform);
			// Move down the line.
			position.setLocation(position.getX(), position.getY() + 2*extent.getY() + pad);
		}
		paintFlusher(graphics, transform, pad);
	}

	private void paintFlusher(Graphics2D graphics, AffineTransform transform, double pad) {
		// TODO Base width on max block size, once we have that constant.
		double maxWidth = 5 + 2*pad;
		double size = Math.min(maxWidth, -bottom);
		pad = 0.1 * size;
		size = size - 2*pad;
		// Build path.
		Path2D path = new Path2D.Double();
		path.moveTo(anchor.getX() + pad, -pad);
		path.lineTo(anchor.getX() + pad + size, -pad);
		path.lineTo(anchor.getX() + pad + size/2, -(size + pad));
		path.closePath();
		// Transform it to display coords.
		path.transform(transform);
		// Fill.
		Color color = Color.getHSBColor(0, 0, 0.2f);
		graphics.setColor(color);
		graphics.fill(path);
		// Draw border.
		graphics.setColor(color.darker());
		graphics.setStroke(new BasicStroke(2));
		graphics.draw(path);
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

	public void setBottom(double bottom) {
		this.bottom = bottom;
	}

}
