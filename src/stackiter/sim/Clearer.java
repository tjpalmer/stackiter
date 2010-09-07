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
	public void paint(Graphics2D graphics, AffineTransform worldRelDisplay) {
		graphics = copy(graphics);
		try {
			Rectangle2D rectangle = rectangle(getPosition(), getExtent());
			rectangle = applied(worldRelDisplay, rectangle);
			// Fill background.
			graphics.setColor(Color.WHITE);
			graphics.fill(rectangle);
			// Draw border.
			graphics.setColor(getColor());
			graphics.setStroke(new BasicStroke(2));
			graphics.draw(rectangle);
			// Draw X.
			// Assume a simple scale on the transform.
			graphics.setStroke(new BasicStroke((float)(0.2 * worldRelDisplay.getScaleX())));
			double reach = 0.7;
			Rectangle2D inset = rectangle(getPosition().getX(), getPosition().getY(), reach * getExtent().getX(), reach * getExtent().getY());
			inset = applied(worldRelDisplay, inset);
			graphics.draw(new Line2D.Double(inset.getMinX(), inset.getMinY(), inset.getMaxX(), inset.getMaxY()));
			graphics.draw(new Line2D.Double(inset.getMinX(), inset.getMaxY(), inset.getMaxX(), inset.getMinY()));
		} finally {
			graphics.dispose();
		}
	}

}
