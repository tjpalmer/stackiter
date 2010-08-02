package stackiter;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;

import org.jbox2d.collision.*;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;

@SuppressWarnings("serial")
public class Stackiter extends JComponent implements ActionListener {

	public static void main(String[] args) {
		JFrame frame = new JFrame("Stackiter");
		frame.setLayout(new BorderLayout());
		Stackiter stackiter = new Stackiter();
		stackiter.setPreferredSize(new Dimension(600, 400));
		frame.add(stackiter, BorderLayout.CENTER);
		frame.pack();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.addNotify();
		frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		stackiter.start();
	}

	private Block block;

	private Block ground;

	private Timer timer;

	private Rectangle2D viewRect;

	private World world;

	public Stackiter() {
		timer = new Timer(10, this);
		viewRect = new Rectangle2D.Double(-20, -5, 40, 25);
		world = new World(new AABB(new Vec2(-100,-100), new Vec2(100,100)), new Vec2(0, -10), true);
		addGround();
		addBlock();
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		// TODO Offload this to a separate thread? If so, still lock step to one update per frame.
		// TODO Alternatively, change the delay based on how much time is left.
		world.step(0.02f, 10);
		repaint();
	}

	private void addBlock() {
		block = new Block();
		block.setColor(Color.getHSBColor(2/3f, 0.7f, 1f));
		block.setExtent(1, 1);
		block.setPosition(0, 10);
		block.setRotation(2 * Math.random() - 1);
		block.addTo(world);
	}

	private void addGround() {
		ground = new Block();
		ground.setColor(Color.getHSBColor(1/12f, 0.5f, 0.5f));
		ground.setDensity(0);
		ground.setExtent(world.getWorldAABB().upperBound.x, 5);
		ground.setPosition(0, -5);
		ground.addTo(world);
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Dimension size = getSize();
		double xScale = size.getWidth() / viewRect.getWidth();
		double yScale = size.getHeight() / viewRect.getHeight();
		double scale = xScale * viewRect.getHeight() > size.getHeight() ? yScale : xScale;
		Graphics2D g = (Graphics2D)graphics.create();
		try {
			g.setColor(Color.WHITE);
			g.fill(getBounds());
			g.translate(0.5 * size.getWidth(), 0.5 * size.getHeight());
			g.scale(scale, -scale);
			g.translate(-viewRect.getCenterX(), -viewRect.getCenterY());
			ground.paint(g);
			block.paint(g);
		} finally {
			g.dispose();
		}
	}

	private void start() {
		timer.start();
	}

}
