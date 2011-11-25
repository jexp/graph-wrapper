package org.neo4j.wrapper;

import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.CombiningIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorUtil;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.Collections.singleton;
import static org.neo4j.helpers.collection.IteratorUtil.firstOrNull;

/**
 * @author mh
 * @since 25.11.11
 */
public class ObjectNode implements Node {
    static final Set<Relationship> NO_RELS = Collections.emptySet();
    private ObjectGraphDatabaseService gdb;
    private Object value;

    public ObjectNode(Object value, ObjectGraphDatabaseService gdb) {
        this.value = value;
        this.gdb = gdb;
    }

    private void setValue(String name, Object newValue) {
        try {
            final Field field = getField(name);
            if (field == null) throw new RuntimeException("No such field " + name);
            field.set(value, newValue);
        } catch (IllegalAccessException e) {
            // ignore
        }
    }

    private Object getValue(String name) {
        final Field field = getField(name);
        if (field == null) return null;
        return getValue(field);
    }

    private Field getField(String name) {
       return getPropertyFields().get(name);
    }

    @Override
    public long getId() {
        return System.identityHashCode(value);
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Relationship> getRelationships() {
        return toRelationships(getRelationshipFields());
    }

    @Override
    public boolean hasRelationship() {
        return isNotEmpty(getRelationships());
    }

    @Override
    public Iterable<Relationship> getRelationships(RelationshipType... relationshipTypes) {
        final Map<String, Field> allRelationshipFields = getRelationshipFields();
        final HashMap<String, Field> relationshipFields = new HashMap<String, Field>();
        for (RelationshipType type : relationshipTypes) {
            relationshipFields.put(type.name(), allRelationshipFields.get(type.name()));
        }
        return toRelationships(relationshipFields);
    }

    @Override
    public Iterable<Relationship> getRelationships(Direction direction, RelationshipType... relationshipTypes) {
        if (direction == Direction.INCOMING) return NO_RELS;
        return getRelationships(relationshipTypes);
    }

    @Override
    public boolean hasRelationship(RelationshipType... relationshipTypes) {
        return hasRelationship(Direction.OUTGOING,relationshipTypes);
    }

    @Override
    public boolean hasRelationship(Direction direction, RelationshipType... relationshipTypes) {
        if (direction == Direction.INCOMING) return false;
        final Iterable<Relationship> values = getRelationships(relationshipTypes);
        return isNotEmpty(values);
    }

    private boolean isNotEmpty(Iterable<Relationship> values) {
        return firstOrNull(values) != null;
    }

    @Override
    public Iterable<Relationship> getRelationships(Direction direction) {
        if (direction == Direction.INCOMING) return NO_RELS;
        final Map<String, Field> relationshipFields = getRelationshipFields();
        return toRelationships(relationshipFields);
    }

    @Override
    public String toString() {
        return String.format("Node[%d]=%s",getId(),value);
    }

    @SuppressWarnings("unchecked")
    private Iterable<Relationship> toRelationships(final Map<String, Field> relationshipFields) {
        return new CombiningIterable<Relationship>(new IterableWrapper<Iterable<Relationship>, Map.Entry<String, Field>>(relationshipFields.entrySet()) {
            @Override
            protected Iterable<Relationship> underlyingObjectToObject(Map.Entry<String, Field> entry) {
                final Object iterableValue = getValue(entry.getValue());
                if (iterableValue instanceof Iterable<?>) {
                    final DynamicRelationshipType relationshipType = DynamicRelationshipType.withName(entry.getKey());
                    return toRelationships(relationshipType, (Iterable<Object>) iterableValue);
                }
                return NO_RELS;
            }
        });
    }

    @Override
    public boolean hasRelationship(Direction direction) {
        if (direction == Direction.INCOMING) return false;
        return !getRelationshipFields().isEmpty(); // todo look at content
    }

    @SuppressWarnings("unchecked")
    public Iterable<Object> getRelationshipValue(RelationshipType relationshipType, Direction direction) {
        if (direction == Direction.INCOMING) return null;
        final String name = relationshipType.name();
        final Field field = getField(name);
        final Object value = getValue(name);
        if (isEntityCollection(field)) return (Iterable<Object>) value;
        if (isEntity(field)) return singleton(value);
        return null;
    }

    private boolean isEntity(Field field) {
        final Class<?> type = field.getType();
        return isEntity(type);
    }

    private boolean isEntity(Class<?> type) {
        if (type.isPrimitive()) return false;
        final Package fieldPackage = type.getPackage();
        if (fieldPackage.getName().startsWith("java.")) return false;
        return fieldPackage.equals(getPackage(value));
    }

    // todo check for primitives, java simple types etc
    private boolean isEntity(Object value) {
        if (value == null) return false;
        return isEntity(value.getClass());
    }

    private Class getActualType(Field field) {
        final Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            final Type actualType = parameterizedType.getActualTypeArguments()[0];
            if (actualType instanceof Class) return (Class)actualType;
        }
        return Object.class;
    }
    private boolean isEntityCollection(Field field) {
        final Class<?> type = field.getType();
        if (!Iterable.class.isAssignableFrom(type)) return false;
        final Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            final Type actualType = parameterizedType.getActualTypeArguments()[0];
            if (actualType instanceof Class) return isEntity((Class)actualType);
        }
        return isEntityCollection(getValue(field));
    }
    private boolean isEntityCollection(Object value) {
        if (!(value instanceof Iterable)) return false;
        final Iterable<Object> iterable = (Iterable<Object>) value;
        final Object first = firstOrNull(iterable);
        return isEntity(first);
    }

    private Package getPackage(Object value) {
        return value.getClass().getPackage();
    }

    @Override
    public Iterable<Relationship> getRelationships(final RelationshipType relationshipType, Direction direction) {
        final Iterable<Object> relationshipValue = getRelationshipValue(relationshipType, direction);
        return toRelationships(relationshipType, relationshipValue);
    }

    private Iterable<Relationship> toRelationships(final RelationshipType relationshipType, final Iterable<Object> relationshipValue) {
        if (relationshipValue == null) return NO_RELS;
        return new IterableWrapper<Relationship, Object>(relationshipValue) {
            @Override
            protected Relationship underlyingObjectToObject(Object other) {
                final ObjectNode otherNode = new ObjectNode(other, gdb);
                return new ObjectRelationship(ObjectNode.this, relationshipType, otherNode, gdb);
            }
        };
    }

    @Override
    public boolean hasRelationship(RelationshipType relationshipType, Direction direction) {
        final Iterable<Object> relationshipValue = getRelationshipValue(relationshipType, direction);
        if (relationshipValue == null) return false;
        return firstOrNull(relationshipValue) != null;
    }

    @Override
    public Relationship getSingleRelationship(RelationshipType relationshipType, Direction direction) {
        return IteratorUtil.singleOrNull(getRelationships(relationshipType, direction));
    }

    @Override
    public Relationship createRelationshipTo(Node node, RelationshipType relationshipType) {
        final String relType = relationshipType.name();
        final Field field = getRelationshipFields().get(relType);
        if (field==null) throw new IllegalArgumentException("Relationship-Type "+ relType+" invalid for fields of type "+getType());
        ObjectNode other = (ObjectNode) node;
        final Object otherValue = other.getValue();
        if (isEntity(field)) {
            if (field.getType().isInstance(otherValue)) {
                setValue(relType, otherValue);
                return new ObjectRelationship(this, relationshipType,other,gdb);
            } else {
                throw new IllegalArgumentException("Relationship-Type "+ relType+" only valid for node types " + field.getType());
            }
        }
        if (isEntityCollection(field)) {
            final Class actualType = getActualType(field);
            if (!actualType.isInstance(otherValue)) throw new IllegalArgumentException("Relationship-Type "+ relType+" only valid for node types " + actualType);

            @SuppressWarnings("unchecked") Collection<Object> values=(Collection<Object>) getValue(field);
            values.add(otherValue);
            return new ObjectRelationship(this,relationshipType, other,gdb);
        }
        throw new IllegalArgumentException("Relationship-Type "+ relType+" only valid for node types " + field.getType());
    }

    @Override
    public Traverser traverse(Traverser.Order order, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, RelationshipType relationshipType, Direction direction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Traverser traverse(Traverser.Order order, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, RelationshipType relationshipType, Direction direction, RelationshipType relationshipType1, Direction direction1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Traverser traverse(Traverser.Order order, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, Object... objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GraphDatabaseService getGraphDatabase() {
        return gdb;
    }

    @Override
    public boolean hasProperty(String name) {
        return getValue(name) != null;
    }

    @Override
    public Object getProperty(String name) {
        return getValue(name);
    }

    @Override
    public Object getProperty(String name, Object defaultValue) {
        final Object value = getValue(name);
        return value != null ? value : defaultValue;
    }

    @Override
    public void setProperty(String name, Object newValue) {
        setValue(name, newValue);
    }

    @Override
    public Object removeProperty(String name) {
        final Object old = getValue(name);
        setValue(name, null);
        return old;
    }

    private Object getValue(Field field) {
        try {
            return field.get(value);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public Iterable<String> getPropertyKeys() {
        return getPropertyFields().keySet();
    }

    @Override
    public Iterable<Object> getPropertyValues() {
        return new IterableWrapper<Object, Field>(getPropertyFields().values()) {
            @Override
            protected Object underlyingObjectToObject(Field field) {
                return getValue(field);
            }
        };
    }

    private Map<String, Field> getPropertyFields() {
        final Class<?> type = value.getClass();
        return gdb.getPropertyFields(type);
    }

    private Map<String, Field> getRelationshipFields() {
        final Class<?> type = value.getClass();
        return gdb.getRelationshipFields(type);
    }

    public Object getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return (int) getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Node) {
            Node other = (Node) obj;
            return getId() == other.getId();
        }
        return false;
    }

    public Iterable<Object> getEndNodeObjects(final Iterable<Relationship> relationships) {
        return new IterableWrapper<Object, Relationship>(relationships) {
            @Override
            protected Object underlyingObjectToObject(Relationship relationship) {
                return ((ObjectNode) relationship.getOtherNode(ObjectNode.this)).getValue();
            }
        };
    }

    public Class<?> getType() {
        return value.getClass();
    }
}
