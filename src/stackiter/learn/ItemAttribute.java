package stackiter.learn;

import java.awt.geom.*;

import sss.*;
import stackiter.sim.*;

public final class ItemAttribute {

	public static final Attribute<Item, java.awt.Color> COLOR =
		new Attribute<Item, java.awt.Color>() {
			@Override
			public java.awt.Color get(Item item) {
				return item.getColor();
			}
		};

	public static class LinearVelocity implements Attribute<Item, Point2D> {
		public Point2D get(Item item) {
			return item.getLinearVelocity();
		}
	}

	public static class Position implements Attribute<Item, Point2D> {
		public Point2D get(Item item) {
			return item.getPosition();
		}
	}

	/**
	 * Don't instantiate me.
	 */
	private ItemAttribute() {}

}
