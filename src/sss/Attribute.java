package sss;

public interface Attribute<Entity, Value> {

	Value get(Entity item);

}
