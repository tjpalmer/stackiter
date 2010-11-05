package stackiter.learn;

import java.util.*;

import stackiter.sim.*;

public class WorldLearnerAgent extends BasicAgent {

	private WorldLearner learner = new WorldLearner();

	@Override
	public void sense() {
		// Um. Why doesn't ArrayList have a constructor from Iterable? Even
		// Iterator should work fine, really.
		List<Item> items = new ArrayList<Item>();
		for (Item item: getWorld().getItems()) {
			// For now, ignore ghost items (in the tray).
			// TODO Reconsider the ghosts.
			if (item.isAlive()) {
				items.add(item.clone());
			}
		}
		// TODO Push across threads to avoid bogging sim? That would make it
		// TODO more robot/real-world-like. If so, handle that here in the
		// TODO agent, in order to keep the actual learner simpler. It will be
		// TODO busy enough over there anyway.
		// TODO
		// TODO Also, if pushing to a separate thread, support the idea of
		// TODO lost updates? That is, if the queue there isn't empty, push a
		// TODO new one anyway? I do want to see consecutive frames, but I
		// TODO want to see them all, actually. Is real-time really the right
		// TODO thing? Somehow, we manage it as people, even though we often
		// TODO think slowly.
		learner.update(items);
	}

}
