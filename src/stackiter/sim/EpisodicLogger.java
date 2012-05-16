package stackiter.sim;

import java.awt.geom.*;


/**
 * Logs the first state and last state of each episode and the clear that
 * delimits them.
 *
 * It also focuses (for now at least) on only primary block state. It doesn't
 * emphasize actions, for example.
 */
public class EpisodicLogger extends AtomicLogger {

	private static class State extends WorldState {

		boolean hasAdds;

		@Override
		public State clone() {
			return (State)super.clone();
		}

		@Override
		public State cloneNew(long steps, double simTime) {
			State result = (State)super.cloneNew(steps, simTime);
			result.hasAdds = false;
			return result;
		}

	}

	private State loggedState;

	private final Logger logger;

	private State oldState;

	private State state;

	public EpisodicLogger(Logger logger) {
		this.logger = logger;
		state = new State();
	}

	@Override
	public void close() {
		logger.close();
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
			if (oldState != null && !state.hasAdds && oldState.hasAdds) {
				doLog(oldState);
			}
		}
		oldState = state;
		// And in with the new.
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

}
