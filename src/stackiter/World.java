package stackiter;

import static stackiter.Util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import org.jbox2d.collision.*;
import org.jbox2d.common.*;

public class World {

	private List<Block> blocks;

	private Block ground;

	private Block graspedBlock;

	private List<Item> items = new ArrayList<Item>();

	private Logger logger;

	private ToolMode toolMode = ToolMode.INACTIVE;

	private ToolMode toolModePrev = ToolMode.INACTIVE;

	private Point2D toolPoint = new Point2D.Double();

	private Tray tray = new Tray();

	private org.jbox2d.dynamics.World world;

	public World() {
		blocks = new ArrayList<Block>();
		world = new org.jbox2d.dynamics.World(new AABB(new Vec2(-100,-100), new Vec2(100,150)), new Vec2(0, -10), true);
		addGround();
		Stock stock = new Stock();
		stock.addTo(this);
		items.add(stock);
	}

	private void addGround() {
		ground = new Block();
		ground.setColor(Color.getHSBColor(0, 0, 0.5f));
		ground.setDensity(0);
		// TODO What's the right way to coordinate display size vs. platform size?
		ground.setExtent(40/2 - 5.5, 5); // 40 == viewRect.getWidth()
		ground.setPosition(0, -5);
		ground.addTo(this);
		items.add(ground);
	}

	public Iterable<Block> getBlocks() {
		// TODO Wrap for immutability?
		return blocks;
	}

	public org.jbox2d.dynamics.World getDynamicsWorld() {
		return world;
	}

	public Item getGraspedItem() {
		return graspedBlock;
	}

	public Block getGround() {
		return ground;
	}

	public Iterable<Item> getItems() {
		// TODO Wrap for immutability?
		return items;
	}

	public Tray getTray() {
		return tray;
	}

	private void handlePress() {
		// Try reserve blocks.
		graspedBlock = tray.graspedBlock(toolPoint);
		if (graspedBlock != null) {
			blocks.add(graspedBlock);
			graspedBlock.addTo(this);
			items.add(graspedBlock);
		}
		if (!tray.isActionConsumed()) {
			for (Block block: blocks) {
				if (block.contains(toolPoint)) {
					graspedBlock = block;
					// Don't break from loop. Make the last drawn have priority for clicking.
					// That's more intuitive when blocks overlap.
					// But how often will that be when physics tries to avoid it?
				}
			}
		}
		if (graspedBlock != null) {
			// No blocks from tray. Try live blocks.
			graspedBlock.grasp(toolPoint);
			Point2D pointRelBlock = appliedInv(graspedBlock.getTransform(), toolPoint);
			logger.logGrasp(graspedBlock, pointRelBlock);
		}
	}

	private void handleRelease() {
		// TODO Log mouse releases independently from grasps. The grasps are cheating.
		if (graspedBlock != null) {
			logger.logRelease(graspedBlock);
			graspedBlock.release();
			graspedBlock = null;
		}
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
		tray.setLogger(logger);
	}

	/**
	 * A component of the agent's action choice.
	 */
	public void setToolMode(ToolMode toolMode) {
		this.toolMode = toolMode;
	}

	/**
	 * A component of the agent's action choice.
	 *
	 * @param toolPoint in the world frame.
	 */
	public void setToolPoint(Point2D toolPoint) {
		this.toolPoint.setLocation(toolPoint);
	}

	public void update() {
		logger.atomic(new Runnable() { @Override public void run() {

			logger.logItem(ground);

			if (toolMode != ToolMode.GRASP) {
				// Check release before moving grasped block.
				handleRelease();
			}
			if (graspedBlock != null) {
				// Move the grasped block, if any.
				graspedBlock.moveTo(toolPoint);
			}
			if (toolMode == ToolMode.GRASP && toolModePrev != ToolMode.GRASP) {
				// But new check grasps after attempting to move any grasped block.
				handlePress();
			}
			toolModePrev = toolMode;

			// Step the simulation.
			world.step(0.02f, 10);

			logger.logMove(toolPoint);

			// Delete lost blocks.
			for (Iterator<Block> b = blocks.iterator(); b.hasNext();) {
				Block block = b.next();
				Shape blockShape = block.transformedShape();
				Rectangle2D blockBounds = blockShape.getBounds2D();
				if (blockBounds.getMaxY() < -5) {
					// It fell off the table. Out of sight, out of mind.
					b.remove();
					block.removeFromWorld();
					logger.logRemoval(block);
				}
			}

			// Record the new state.
			logger.logItem(ground);
			for (Block block: blocks) {
				logger.logItem(block);
			}

		}});
	}

}
