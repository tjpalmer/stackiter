package stackiter.sim;

import java.util.*;

/**
 * Context-dependent information for logging.
 */
public class Meta {

	/**
	 * List of arguments.
	 * Special treatment is given to points and to souls of world items.
	 *
	 * TODO Just use toString on other objects and wrap them (escaped) in
	 * TODO quotes?
	 */
	public List<Object> args;

	/**
	 * The name of this metadata.
	 */
	public String name;

	public Meta(String name, Object... args) {
		this.name = name;
		this.args = new ArrayList<Object>();
		this.args.addAll(Arrays.asList(args));
	}

}
