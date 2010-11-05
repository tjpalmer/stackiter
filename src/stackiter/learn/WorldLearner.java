package stackiter.learn;

import jamasam.*;

import java.awt.geom.*;
import java.util.*;

import stackiter.sim.*;

public class WorldLearner {

	private Map<Soul, Integer> ids = new HashMap<Soul, Integer>();

	public void update(List<Item> items) {

		// Get the current IDs in order, using just one hashtable lookup. Then
		// we can decouple the different attribute pulls without needing more
		// lookups again.
		int[] ids = new int[items.size()];
		for (int i = 0; i < items.size(); i++) {
			ids[i] = idFor(items.get(i).getSoul());
		}

		final int ndims = 2;
		Matrix positions = new Matrix(ndims, items.size());
		for (int j = 0; j < positions.getN(); j++) {
			Item item = items.get(j);
			Point2D position = item.getPosition();
			positions.set(0, ids[j], position.getX());
			positions.set(1, ids[j], position.getY());
		}

	}

	private int idFor(Soul soul) {
		Integer id = ids.get(soul);
		if (id == null) {
			// Just increment for now. Reconsider later.
			id = ids.size() + 1;
			ids.put(soul, id);
		}
		return id;
	}

}
