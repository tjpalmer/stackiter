package sss;

public abstract class DoubleAttribute<Entity> implements Attribute<Entity, Double> {

	@Override
	public Double get(Entity entity) {
		return getDouble(entity);
	}

	public abstract double getDouble(Entity entity);

}
