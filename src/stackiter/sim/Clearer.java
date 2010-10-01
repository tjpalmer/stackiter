package stackiter.sim;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;

public class Clearer extends BasicItem {

	public Clearer() {
		setColor(Color.RED);
		setExtent(point(1, 1));
	}

	@Override
	public void paint(Graphics2D graphics) {
		graphics = copy(graphics);
		try {
			Rectangle2D rectangle = rectangle(getPosition(), getExtent());
			// Fill background.
			graphics.setColor(Color.WHITE);
			graphics.fill(rectangle);
			// Draw border.
			graphics.setColor(getColor());
			graphics.setStroke(new BasicStroke(0.2f));
			graphics.draw(rectangle);
			// Draw X.
			// Assume a simple scale on the transform.
			double reach = 0.7;
			Rectangle2D inset = rectangle(getPosition().getX(), getPosition().getY(), reach * getExtent().getX(), reach * getExtent().getY());
			graphics.draw(new Line2D.Double(inset.getMinX(), inset.getMinY(), inset.getMaxX(), inset.getMaxY()));
			graphics.draw(new Line2D.Double(inset.getMinX(), inset.getMaxY(), inset.getMaxX(), inset.getMinY()));
		} finally {
			graphics.dispose();
		}
	}

}
