package stackiter.sim;

import java.awt.geom.*;
import java.util.*;


/**
 * Logs the first state and last state of each episode and the clear that
 * delimits them.
 *
 * It also focuses (for now at least) on only primary block state. It doesn't
 * emphasize actions, for example.
 */
public class EpisodicLogger extends AtomicLogger {

	private static class State extends WorldState {

		boolean episodeStart;

		boolean hasAdds;

		List<Meta> metas = new ArrayList<Meta>();

		@Override
		public State clone() {
			return (State)super.clone();
		}

		@Override
		public State cloneNew(long steps, double simTime) {
			State result = (State)super.cloneNew(steps, simTime);
			// These transitory events are in relation to a particular state.
			// TODO It would be easier just to make a new object and copy only
			// TODO the items.
			result.episodeStart = false;
			result.hasAdds = false;
			result.metas = new ArrayList<Meta>();
			return result;
		}

	}

	private State loggedState;

	private final Logger logger;

	private State oldState;

	private State state;

	private boolean doWaitForEpisodeStart;

	public EpisodicLogger(Logger logger) {
		this.logger = logger;
		state = new State();
	}

	@Override
	public void close() {
		logger.close();
	}

	@Override
	public Logger getKid() {
		return logger;
	}

	private void doLog(State state) {
		// Actual logging of key info.
		logger.logSimTime(state.steps, state.simTime);
		if (state.cleared) {
			logger.logClear();
		}
		// Items. First removals, then new/updated items.
		if (loggedState != null) {
			for (Item logged: loggedState.items.values()) {
				if (!state.items.containsKey(logged.getSoul())) {
					logger.logRemoval(logged);
				}
			}
		}
		for (Item item: state.items.values()) {
			logger.logItem(item);
		}
		// Metas all need to go out.
		for (Meta meta: state.metas) {
			logger.logMeta(meta);
		}
		// Remember exactly what we logged for more reliability.
		loggedState = state.clone();
	}

	@Override
	public void flush() {
		logger.flush();
	}

	@Override
	public void logClear() {
		// Actual removal is logged separately.
		state.cleared = true;
		// We do need to log the state before the clear.
		doLog(oldState);
	}

	@Override
	public void logDisplaySize(Point2D size) {
		// Ignore for now.
	}

	@Override
	public void logEpisodeStart() {
		state.episodeStart = true;
	}

	@Override
	public void logGrasp(Tool tool, Block item, Point2D pointRelItem) {
		// Ignore for now.
	}

	@Override
	public void logItem(Item item) {
		// Could try to be intelligent about updates, but this will do.
		if (!state.items.containsKey(item.getSoul())) {
			state.hasAdds = true;
		}
		state.items.put(item.getSoul(), item.clone());
	}

	@Override
	public void logMeta(Meta meta) {
		state.metas.add(meta);
	}

	@Override
	public void logRelease(Tool tool, Block item) {
		// Ignore for now.
	}

	@Override
	public void logRemoval(Item item) {
		state.items.remove(item.getSoul());
	}

	@Override
	public void logSimTime(long steps, double seconds) {
		// Out with the old.
		if (state != null) {
			if (state.cleared) {
				// We always want to log a cleared state and the new one after it.
				// TODO Do we need to wait for blocks to be added and moving?
				doLog(state);
			}
			if (oldState != null && (
				oldState.episodeStart ||
				// Assume metas matter for now.
				// I need that for options.
				// TODO Track flush priority on metas?
				!oldState.metas.isEmpty() ||
				// TODO Remove the following heuristic and base always on episodes?
				// TODO Well, and metas need flushed, too.
				(!doWaitForEpisodeStart && !state.hasAdds && oldState.hasAdds)
			)) {
				doLog(oldState);
			}
		}
		oldState = state;
		// And in with the new.
		// TODO Apparently we don't believe state is ever null, despite the
		// TODO earlier check.
		state = state.cloneNew(steps, seconds);
	}

	@Override
	public void logTool(Tool tool) {
		// Ignore for now.
	}

	@Override
	public void logToolPresent(Tool tool, boolean toolPresent) {
		// Ignore for now.
	}

	@Override
	public void logTray(Tray tray) {
		// Ignore for now.
	}

	@Override
	public void logView(Rectangle2D view) {
		// Ignore for now.
	}

	@Override
	public void waitForEpisodeStart() {
		this.doWaitForEpisodeStart = true;
	}

}
