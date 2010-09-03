package stackiter.sim;

import java.awt.*;
import java.awt.geom.*;

public interface Item {

	public abstract void paint(Graphics2D graphics, AffineTransform worldRelDisplay);

}
