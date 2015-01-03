package stackiter.sim;

import static stackiter.sim.Util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import org.jbox2d.collision.*;
import org.jbox2d.common.*;

public class World {

	/**
	 * For tracking extra item information not stored directly in the items.
	 */
	private class ItemInfo {

		Item item;

		/**
		 * TODO Full old state at some point?
		 */
		double oldAngularVelocity;

		/**
		 * TODO Full old state at some point?
		 */
		Point2D oldLinearAcceleration = point();

		/**
		 * TODO Full old state at some point?
		 */
		Point2D oldLinearVelocity = point();

	}

	/**
	 * For tracking internal (and previous) states of tools.
	 */
	private class ToolInfo {

		Block graspedItem;

		Tool old = new Tool();

	}

	/**
	 * We run the sim longer than we claim. This makes it feel snappier.
	 *
	 * TODO Maybe a different physical scale would also have worked?
	 *
	 * TODO Make this a member of World instances instead of a global?
	 */
	public static final double TIME_SCALE = 2;

	private List<Agent> agents = new ArrayList<Agent>();

	private List<Block> blocks;

	private int clearCount;

	private Clearer clearer;

	/**
	 * Whether the current state is the start of an episode, where that's
	 * applicable.
	 */
	private boolean episodeStarted;

	private Block ground;

	private List<ItemInfo> items = new ArrayList<ItemInfo>();

	private Logger logger;

	private boolean quitRequested;

	private long steps;

	private Map<Tool, ToolInfo> tools = new HashMap<Tool, ToolInfo>();

	private Tray tray = new Tray();

	private double trayHeight = 30.0;

	private org.jbox2d.dynamics.World world;

	public World() {
		blocks = new ArrayList<Block>();
		world = new org.jbox2d.dynamics.World(
			new AABB(new Vec2(-200,-100), new Vec2(200,400)),
			new Vec2(0, -10),
			true
		);
		addGround();
		//Stock stock = new Stock();
		//stock.addTo(this);
		//items.add(stock);
	}

	public void addAgent(Agent agent) {
		agents.add(agent);
		agent.setWorld(this);
	}

	public void addBlock(Block block) {
		blocks.add(block);
		block.addTo(this);
		addItem(block);
	}

	private void addClearer() {
		clearer = new Clearer();
		double offset = -ground.getExtent().getY();
		clearer.setPosition(point(ground.getExtent().getX() + offset, offset));
		addItem(clearer);
	}

	private void addGround() {
		ground = new Block();
		ground.setColor(Color.getHSBColor(0, 0, 0.5f));
		ground.setDensity(0);
		// TODO What's the right way to coordinate display size vs. platform size?
		ground.setExtent(40/2, 1.5); // 40 == viewRect.getWidth()
		ground.setPosition(0, -1.5);
		// Actually add the ground later, so other folks have a chance to modify
		// it first.
	}

	private void addItem(Item item) {
		ItemInfo info = new ItemInfo();
		info.item = item;
		items.add(info);
	}

	/**
	 * Agents affect world state by means of tools. That is, actions are
	 * expressed here. Often, an agent has one tool.
	 */
	public Tool addTool() {
		Tool tool = new Tool();
		tools.put(tool, new ToolInfo());
		return tool;
	}

	public void clearBlocks() {
		for (Block block: blocks) {
			// Calling this in a block loop is super inefficient n^2.
			handleRemoval(block);
		}
		blocks.clear();
		logger.logClear();
		clearCount++;
	}

	/**
	 * If the tool has a block grasped, it shouldn't be able to move past the
	 * block. Alternatively, if we let it slip some, it should drop the block if
	 * it gets too far.
	 *
	 * The point of this is to allow physical cues to the learning system that
	 * there is a relationship between the tool and the item without having to
	 * remember the past state of the tool clicking on the particular item. That
	 * is, I'm trying to avoid hidden state at the moment. Confusion is still
	 * possible, but it probably improves the ability to have good evidence for
	 * the relationship.
	 */
	private void constrainTool(Tool tool, Block graspedItem) {
		if (graspedItem == null) {
			return;
		}
		Point2D graspedPoint = graspedItem.getGraspPosition();
		Point2D toolPoint = applied(graspedItem.getTransform(), graspedPoint);
		tool.setPosition(toolPoint);
	}

	public void episodeStarted() {
		this.episodeStarted = true;
	}

	public Iterable<Block> getBlocks() {
		// TODO Wrap for immutability?
		return blocks;
	}

	public int getClearCount() {
		return clearCount;
	}

	public Clearer getClearer() {
		for (Item item: getItems()) {
			if (item instanceof Clearer) {
				return (Clearer)item;
			}
		}
		return null;
	}

	public org.jbox2d.dynamics.World getDynamicsWorld() {
		return world;
	}

	public Item getGraspedItem(Tool tool) {
		return tools.get(tool).graspedItem;
	}

	public Block getGround() {
		return ground;
	}

	/**
	 * Gets a list of the world items in the world coordination frame.
	 */
	public Iterable<Item> getItems() {
		// TODO Improve this to avoid copying and nonsense?
		// TODO Or should I guarantee copies to avoid modification???
		List<Item> items = new ArrayList<Item>();
		for (Block block: tray.getItems()) {
			Block copied = block.clone();
			copied.setPosition(added(copied.getPosition(), tray.getAnchor()));
			items.add(copied);
		}
		for (ItemInfo info: this.items) {
			items.add(info.item);
		}
		return items;
	}

	public List<Soul> getItemSouls() {
		List<Soul> itemSouls = new ArrayList<Soul>();
		for (Item item: getItems()) {
			itemSouls.add(item.getSoul());
		}
		return itemSouls;
	}

	public Logger getLogger() {
		return logger;
	}

	/**
	 * The amount of simulation time that has passed in seconds.
	 */
	public double getSimTime() {
		return getStepTime() * steps;
	}

	/**
	 * The amount of sim time that we claim passes for each update.
	 */
	public double getStepTime() {
		// Just hardcoded for now.
		return 0.01;
	}

	public Tray getTray() {
		return tray;
	}

	private void handlePress(Tool tool) {
		// Check for clearing the screen.
		// TODO Generify widget concept?
		if (clearer.contains(tool.getPosition())) {
			clearBlocks();
			return;
		}
		// Try reserve blocks.
		ToolInfo toolInfo = tools.get(tool);
		Block graspedBlock = tray.graspedBlock(tool.getPosition());
		if (graspedBlock != null) {
			addBlock(graspedBlock);
		}
		if (!tray.isActionConsumed()) {
			for (Block block: blocks) {
				if (block.contains(tool.getPosition())) {
					if (!block.isGrasped()) {
						// For now, don't allow double-grasping.
						// TODO Support double-grasping.
						graspedBlock = block;
					}
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
		// Remember the block we got (or didn't).
		toolInfo.graspedItem = graspedBlock;
	}

	private void handleRelease(Tool tool) {
		ToolInfo toolInfo = tools.get(tool);
		if (toolInfo.graspedItem != null) {
			logger.logRelease(tool, toolInfo.graspedItem);
			// TODO What if multiple tools have the same block?
			toolInfo.graspedItem.release();
			toolInfo.graspedItem = null;
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
		// Release grasps.
		// TODO This isn't super efficient, since handleRelease does a relookup. Consider reorg.
		for (Map.Entry<Tool, ToolInfo> entry: tools.entrySet()) {
			if (entry.getValue().graspedItem == block) {
				handleRelease(entry.getKey());
			}
		}
		// Remove the block.
		block.removeFromWorld();
		logger.logRemoval(block);
		// This isn't efficient to step through one at a time.
		ITEMS: for (Iterator<ItemInfo> i = items.iterator(); i.hasNext();) {
			ItemInfo info = i.next();
			if (info.item == block) {
				i.remove();
				break ITEMS;
			}
		}
	}

	public boolean isQuitRequested() {
		return quitRequested;
	}

	public void paint(Graphics2D graphics) {
		// Live items.
		paintItems(graphics);
		// Tray. Includes preborn blocks.
		tray.paint(graphics);
		// Tools. Paint these after items on purpose.
		for (Tool tool: tools.keySet()) {
			tool.paint(graphics);
		}
	}

	public void paintItems(Graphics2D graphics) {
		for (ItemInfo info: items) {
			info.item.paint(graphics);
		}
	}

	/**
	 * Properly removes a block from the world.
	 */
	public void removeBlock(Block block) {
		handleRemoval(block);
		blocks.remove(block);
	}

	public void requestQuit() {
		quitRequested = true;
	}

	public void setLogger(Logger logger) {
		// Make sure the ground and clearer get added first.
		// TODO It seems really horrid to tie this init to the logger.
		ground.addTo(this);
		addItem(ground);
		// Add the clearer after the ground so it paints later and so on.
		addClearer();

		this.logger = logger;
		tray.setLogger(logger);
		// Log the ground right away because I like a low number for it.
		logger.logItem(ground);

		// Tray.
		// It logs on fill. That's why the setup is here.
		// TODO Figure out a better organization for logger vs. tray.
		// TODO Maybe tray logger setting could log its current state immediately despite prior setup?
		double minX = ground.getPosition().getX() - ground.getExtent().getX();
		tray.setAnchor(point(minX - 12.5, 0));
		tray.setHeight(trayHeight);
		if (trayHeight == 0) {
			tray.fill();
		}
	}

	/**
	 * The tray height is configured after scenario init, so store the desired
	 * height here for later.
	 */
	public void setTrayHeight(double trayHeight) {
		this.trayHeight = trayHeight;
	}

	public void update() {
		logger.atomic(new Runnable() { @Override public void run() {

			// Let agents choose actions.
			for (Agent agent: agents) {
				agent.act();
			}

			// Find out what actions they are.
			for (Tool tool: tools.keySet()) {
				// Log tools after world update, because of constraints.
				// Just handle grasp/release here, so it can affect the world.
				// Note that we now hide the "force" state if we don't log attempted tool position.
				ToolInfo toolInfo = tools.get(tool);
				if (tool.getMode() != ToolMode.GRASP) {
					// Check release before moving grasped block.
					handleRelease(tool);
				}
				if (toolInfo.graspedItem != null) {
					// Move the grasped block, if any.
					toolInfo.graspedItem.moveTo(tool.getPosition());
				}
				if (tool.getMode() == ToolMode.GRASP && toolInfo.old.getMode() != ToolMode.GRASP) {
					// But new check grasps after attempting to move any grasped block.
					handlePress(tool);
				}
				toolInfo.old.setMode(tool.getMode());
			}

			// Step the simulation.
			// We step twice the claimed step time because things feel snappier that way.
			world.step((float)(TIME_SCALE * getStepTime()), 10);
			steps++;
			// We log half sim time since that's also what we show the user.
			// Might make for odd gravity perhaps or whatnot, but whatever.
			logger.logSimTime(steps, getSimTime());

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

			// Update accelerations (something the engine doesn't track for us).
			for (ItemInfo info: items) {
				double scale = 1 / getStepTime();
				// Update acceleration.
				Point2D linearAcceleration = scaled(scale, subtracted(info.item.getLinearVelocity(), info.oldLinearVelocity));
				info.item.setLinearAcceleration(linearAcceleration);
				double angularAcceleration = scale * (info.item.getAngularVelocity() - info.oldAngularVelocity);
				info.item.setAngularAcceleration(angularAcceleration);
				// Now with that, update jerk.
				Point2D linearJerk = scaled(scale, subtracted(info.item.getLinearAcceleration(), info.oldLinearAcceleration));
				info.item.setLinearJerk(linearJerk);
				// Now update the old values for next time.
				info.oldAngularVelocity = info.item.getAngularVelocity();
				info.oldLinearAcceleration.setLocation(info.item.getLinearAcceleration());
				info.oldLinearVelocity.setLocation(info.item.getLinearVelocity());
				//System.out.println(info.item.getColor() + ": " + info.item.getLinearAcceleration() + " and " + info.item.getLinearJerk());
			}

			// Constrain and log tool states.
			for (Map.Entry<Tool, ToolInfo> t: tools.entrySet()) {
				Tool tool = t.getKey();
				constrainTool(tool, t.getValue().graspedItem);
				logger.logTool(tool);
			}

			// Record the new state.
			logger.logItem(ground);
			for (Block block: blocks) {
				logger.logItem(block);
			}

			// Say it was a new episode start, if indicated.
			if (episodeStarted) {
				logger.logEpisodeStart();
				// Done said that now.
				episodeStarted = false;
			}

			// Tell the agents what's up now, in case they care before next update.
			for (Agent agent: agents) {
				agent.sense();
			}

		}});
	}

}
