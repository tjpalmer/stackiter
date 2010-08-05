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

	public Tray(int total) {
		blocks = new ArrayList<Block>();
		random = new Random();
		while (blocks.size() < total) {
			blocks.add(randomBlock());
		}
	}

	public void paint(Graphics2D graphics, AffineTransform transform) {
		Point2D position = copy(anchor);
		for (Block block: blocks) {
			Point2D extent = block.getExtent();
			// Pad the block.
			extent.setLocation(extent.getX() + 0.2, extent.getY() + 0.1);
			// Position and paint it.
			block.setPosition(position.getX() + extent.getX(), position.getY() - extent.getY());
			block.paint(graphics, copy(transform));
			// Move down the line.
			position.setLocation(position.getX(), position.getY() - 2 * extent.getY());
		}
	}

	private Block randomBlock() {
		Block block = new Block();
		block.setColor(randomColor());
		// TODO Consider Gaussian mixture model on sizes.
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
