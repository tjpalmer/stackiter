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
		// TODO Auto-generated method stub
		super.paint(graphics, worldRelDisplay);
	}

}
