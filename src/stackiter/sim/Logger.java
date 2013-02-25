package stackiter.sim;

import java.awt.geom.*;
import java.io.*;

/**
 * For logging states (including actions) for later analysis.
 *
 * TODO Loggers to file, ROS, Matlab, and web server all seem possible.
 */
public interface Logger extends Closeable {

	/**
	 * Treats all logs within the runnable as occurring at the same clock time.
	 */
	void atomic(Runnable runnable);

	void close();

	void flush();

	/**
	 * @param size pixel width and height as integer values, despite use of Point2D.
	 */
	void logDisplaySize(Point2D size);

	/**
	 * The clear button was used.
	 *
	 * Just for convenience when interpreting logs.
	 */
	void logClear();

	/**
	 * An episode is now constructed and ready to start.
	 */
	void logEpisodeStart();

	void logGrasp(final Tool tool, final Block item, final Point2D pointRelItem);

	void logItem(final Item item);

	/**
	 * Log abstract information dependent on the context.
	 */
	void logMeta(Meta meta);

	void logRelease(final Tool tool, final Block item);

	void logRemoval(final Item item);

	/**
	 * Logs both step count and sim time. JBox2D requires a constant step time,
	 * but this interface here technically allows for nonconstant.
	 */
	void logSimTime(long steps, double seconds);

	void logTool(final Tool tool);

	/**
	 * Really, this is about mouse tools. From a UI perspective, it might be
	 * nice to know when the mouse comes or goes from the window, even if that
	 * doesn't directly affect world state.
	 */
	void logToolPresent(Tool tool, boolean toolPresent);

	void logTray(Tray tray);

	void logView(final Rectangle2D view);

	/**
	 * Tell the logger that episodes (for loggers that care) don't start until
	 * logEpisodeStart has occurred. By default, episode start is heuristically
	 * judged for some point after clear.
	 *
	 * TODO Change to a setter instead of a permanent switch?
	 */
	void waitForEpisodeStart();

}
