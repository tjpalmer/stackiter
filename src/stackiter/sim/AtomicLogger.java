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

	protected int getTxDepth() {
		return txDepth;
	}

}
