package stackiter.sim;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import org.jbox2d.collision.*;
import org.jbox2d.common.*;

public class World {

	private List<Block> blocks;

	private Clearer clearer;

	private Block ground;

	private Block graspedBlock;

	private List<Item> items = new ArrayList<Item>();

	private Logger logger;

	private Map<Tool, ToolMode> toolModePrevs = new HashMap<Tool, ToolMode>();

	private List<Tool> tools = new ArrayList<Tool>();

	private Tray tray = new Tray();

	private org.jbox2d.dynamics.World world;

	public World() {
		blocks = new ArrayList<Block>();
		world = new org.jbox2d.dynamics.World(new AABB(new Vec2(-100,-100), new Vec2(100,150)), new Vec2(0, -10), true);
		addGround();
		// Add the clearer after the ground so it paints later and so on.
		addClearer();
		//Stock stock = new Stock();
		//stock.addTo(this);
		//items.add(stock);
	}

	private void addClearer() {
		clearer = new Clearer();
		double offset = -ground.getExtent().getY();
		clearer.setPosition(point(ground.getExtent().getX() + offset, offset));
		items.add(clearer);
	}

	private void addGround() {
		ground = new Block();
		ground.setColor(Color.getHSBColor(0, 0, 0.5f));
		ground.setDensity(0);
		// TODO What's the right way to coordinate display size vs. platform size?
		ground.setExtent(40/2, 1.5); // 40 == viewRect.getWidth()
		ground.setPosition(0, -1.5);
		ground.addTo(this);
		items.add(ground);
	}

	/**
	 * Agents affect world state by means of tools. That is, actions are
	 * expressed here. Generally, each agent should have a tool.
	 */
	public Tool addTool() {
		Tool tool = new Tool();
		tools.add(tool);
		toolModePrevs.put(tool, ToolMode.INACTIVE);
		// TODO Means for tracking old tool position, too?
		// TODO We might want to limit how far the tool really can move in a time step.
		return tool;
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

	/**
	 * Gets a list of the world items in the world coordination frame.
	 */
	public Iterable<Item> getItems() {
		// TODO Improve this to avoid copying and nonsense?
		List<Item> items = new ArrayList<Item>();
		for (Block block: tray.getItems()) {
			Block copied = block.clone();
			copied.setPosition(added(copied.getPosition(), tray.getAnchor()));
		}
		items.addAll(this.items);
		return items;
	}

	public Tray getTray() {
		return tray;
	}

	private void handlePress(Tool tool) {
		// Check for clearing the screen.
		// TODO Generify widget concept?
		if (clearer.contains(tool.getPosition())) {
			for (Block block: blocks) {
				handleRemoval(block);
			}
			blocks.clear();
			return;
		}
		// Try reserve blocks.
		graspedBlock = tray.graspedBlock(tool.getPosition());
		if (graspedBlock != null) {
			blocks.add(graspedBlock);
			graspedBlock.addTo(this);
			items.add(graspedBlock);
		}
		if (!tray.isActionConsumed()) {
			for (Block block: blocks) {
				if (block.contains(tool.getPosition())) {
					graspedBlock = block;
					// Don't break from loop. Make the last drawn have priority for clicking.
					// That's more intuitive when blocks overlap.
					// But how often will that be when physics tries to avoid it?
				}
			}
		}
		if (graspedBlock != null) {
			// No blocks from tray. Try live blocks.
			graspedBlock.grasp(tool.getPosition());
			Point2D pointRelBlock = appliedInv(graspedBlock.getTransform(), tool.getPosition());
			logger.logGrasp(tool, graspedBlock, pointRelBlock);
		}
	}

	private void handleRelease(Tool tool) {
		// TODO Log mouse releases independently from grasps. The grasps are cheating.
		if (graspedBlock != null) {
			logger.logRelease(tool, graspedBlock);
			graspedBlock.release();
			graspedBlock = null;
		}
	}

	/**
	 * Doesn't actually remove from the block list but from the sim and also
	 * logs.
	 *
	 * It _does_ remove it from items.
	 *
	 * The issue here is that we often call this method while iterating blocks,
	 * so that makes it hard to do that removal here.
	 */
	private void handleRemoval(Block block) {
		block.removeFromWorld();
		logger.logRemoval(block);
		items.remove(block);
	}

	public void paint(Graphics2D graphics, AffineTransform worldRelDisplay) {
		// Live items.
		for (Item item: items) {
			item.paint(graphics, worldRelDisplay);
		}
		// Tray. Includes preborn blocks.
		tray.paint(graphics, worldRelDisplay);
		// Tools. Paint these after items on purpose.
		for (Tool tool: tools) {
			tool.paint(graphics, worldRelDisplay);
		}
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
		tray.setLogger(logger);
		// Log the ground right away because I like a low number for it.
		logger.logItem(ground);
	}

	public void update() {
		logger.atomic(new Runnable() { @Override public void run() {

			for (Tool tool: tools) {
				logger.logTool(tool);
				if (tool.getMode() != ToolMode.GRASP) {
					// Check release before moving grasped block.
					handleRelease(tool);
				}
				if (graspedBlock != null) {
					// Move the grasped block, if any.
					graspedBlock.moveTo(tool.getPosition());
				}
				ToolMode toolModePrev = toolModePrevs.get(tool);
				if (tool.getMode() == ToolMode.GRASP && toolModePrev != ToolMode.GRASP) {
					// But new check grasps after attempting to move any grasped block.
					handlePress(tool);
				}
				toolModePrevs.put(tool, tool.getMode());
			}

			// Step the simulation.
			world.step(0.02f, 10);

			// Delete lost blocks.
			for (Iterator<Block> b = blocks.iterator(); b.hasNext();) {
				Block block = b.next();
				Shape blockShape = block.transformedShape();
				Rectangle2D blockBounds = blockShape.getBounds2D();
				if (blockBounds.getMaxY() < -5) {
					// It fell off the table. Out of sight, out of mind.
					b.remove();
					handleRemoval(block);
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
