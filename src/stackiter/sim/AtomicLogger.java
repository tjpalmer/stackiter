package stackiter.sim;

/**
 * Provides generic atomic support as a convenience.
 */
public abstract class AtomicLogger implements Logger {

	private int txDepth;

	@Override
	public void atomic(Runnable runnable) {
		beginStep();
		try {
			runnable.run();
		} finally {
			endStep();
		}
	}

	protected void beginStep() {
		txDepth++;
	}

	protected void endStep() {
		txDepth--;
	}

	/**
	 * Be sure to override if there's a kid!
	 */
	@Override
	public Logger getKid() {
		return null;
	}

	protected int getTxDepth() {
		return txDepth;
	}

	@Override
	public void logEpisodeStart() {
		// For convenience for subclasses, do nothing here.
	}

	@Override
	public void push() {
		// For convenience for subclasses, do nothing here.
		// Subclasses that hold back should push here.
	}

	@Override
	public void waitForEpisodeStart() {
		// For convenience for subclasses, do nothing here.
	}

}
