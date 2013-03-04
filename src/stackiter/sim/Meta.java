package stackiter.sim;

import java.util.*;

/**
 * Context-dependent information for logging.
 */
public class Meta {

	/**
	 * Something that provides metadata.
	 */
	public interface Provider {
		/**
		 * Loggable meta-description of the object.
		 * The returned Meta must be new, as it might be modified later, at
		 * least at the name and args list level.
		 * Args themselves might not be modifiable.
		 */
		Meta meta();
	}

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
