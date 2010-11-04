package stackiter.learn;

import java.awt.geom.*;

import sss.*;
import stackiter.sim.*;

public final class ItemAttribute {

	public static final DoubleAttribute<Item> ANGLE =
		new DoubleAttribute<Item>() {
			@Override
			public double getDouble(Item item) {
				return item.getAngle();
			}
		};

	public static final Attribute<Item, java.awt.Color> COLOR =
		new Attribute<Item, java.awt.Color>() {
			@Override
			public java.awt.Color get(Item item) {
				return item.getColor();
			}
		};

	public static final Attribute<Item, Point2D> EXTENT =
		new Attribute<Item, Point2D>() {
			@Override
			public Point2D get(Item item) {
				return item.getExtent();
			}
		};

	public static final Attribute<Item, Point2D> LINEAR_VELOCITY =
		new Attribute<Item, Point2D>() {
		@Override
			public Point2D get(Item item) {
				return item.getLinearVelocity();
			}
		};

	public static final Attribute<Item, Point2D> POSITION =
		new Attribute<Item, Point2D>() {
			@Override
			public Point2D get(Item item) {
				return item.getPosition();
			}
		};

	/**
	 * Don't instantiate me.
	 */
	private ItemAttribute() {}

}
