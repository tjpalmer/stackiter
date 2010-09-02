package stackiter;

import static stackiter.Util.*;

import java.awt.*;
import java.awt.geom.*;
//import java.util.*;
//import java.util.List;

/**
 * Provides an in-world supply of new items.
 */
public class Stock implements Item {

	private Block base = new Block();

	private Block casing = new Block();

	private Point2D casingOffset = point(0, 7.5);

	//private Block roof = new Block();

	// TODO private List<Block> blocks = new ArrayList<Block>();

	public Stock() {
		// Build and position the base.
		base.setColor(Color.getHSBColor(0, 0, 0.7f));
		base.setExtent(3, 1.25);
		base.setAngle(0.15);
		base.setPosition(0, base.getExtent().getY() + 15);
		// Build the casing.
		casing.setColor(Color.WHITE);
		casing.setExtent(base.getExtent().getX(), casingOffset.getY());
		updateCasing();
		// Build the roof relative to the base.
		//		roof.setColor(base.getColor());
		//		roof.setExtent(base.getExtent().getX(), 0.5);
		//		roof.setPosition(
		//			base.getPosition().getX(),
		//			// 10 units of space for displaying blocks.
		//			15 + base.getPosition().getY() + base.getExtent().getY() + roof.getExtent().getY()
		//		);
		//		// Constrain them relative to each other.
		//		base.affix(roof);
	}

	private void updateCasing() {
		AffineTransform transform = base.getTransform();
		Point2D position = point(casingOffset.getX(), casingOffset.getY() + base.getExtent().getY());
		position = applied(transform, position);
		casing.setAngle(base.getAngle());
		casing.setPosition(position.getX(), position.getY());
	}

	void addTo(World world) {
		base.addTo(world);
		//		roof.addTo(world);
	}

	@Override
	public void paint(Graphics2D graphics, AffineTransform worldRelDisplay) {
		//		roof.paint(graphics, worldRelDisplay);
		updateCasing();
		base.paint(graphics, worldRelDisplay);
		casing.paint(graphics, worldRelDisplay);
		// TODO Paint button.
	}

}
