package sss;

public abstract class DoubleAttribute<Item> implements Attribute<Item, Double> {

	@Override
	public Double get(Item item) {
		return getDouble(item);
	}

	public abstract double getDouble(Item item);

}
