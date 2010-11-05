package stackiter.learn;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Loader {

	/**
	 * Instance level so we don't need to pass in args.
	 */
	private Map<String, Runnable> handlers;

	/**
	 * The sequence of loaded states.
	 */
	private Sequence sequence;

	/**
	 * The current state of the world, needing cloned and frequently modified
	 * for each new time step.
	 */
	private State state;

	public Loader() {
		clear();
		handlers = new HashMap<String, Runnable>();
		handlers.put("color", new Runnable() { @Override public void run() {
			// TODO Auto-generated method stub
		}});
		handlers.put("destroy", new Runnable() { @Override public void run() {
			// TODO Auto-generated method stub
		}});
		handlers.put("extent", new Runnable() { @Override public void run() {
			// TODO Auto-generated method stub
		}});
		handlers.put("item", new Runnable() { @Override public void run() {
			// TODO Auto-generated method stub
		}});
		handlers.put("pos", new Runnable() { @Override public void run() {
			// TODO Auto-generated method stub
		}});
		handlers.put("rot", new Runnable() { @Override public void run() {
			// TODO Auto-generated method stub
		}});
	}

	private void clear() {
		sequence = new Sequence();
		state = new State();
	}

	public Sequence load(String name) {
		try {
			InputStream stream = new FileInputStream(name);
			try {
				if (name.endsWith(".gz")) {
					stream = new GZIPInputStream(stream);
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(stream));
				String line;
				while ((line = in.readLine()) != null) {
					parseLine(line);
				}
				return sequence;
			} finally {
				stream.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void parseLine(String line) {
		// TODO Look into faster splitting than regex?
		String[] words = line.split("\\s+");
		String command = words[0];
		Runnable handler = handlers.get(command);
		if (handler != null) {
			handler.run();
		}
	}

}
