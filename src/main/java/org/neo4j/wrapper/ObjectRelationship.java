package org.neo4j.wrapper;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.Collections;

/**
 * @author mh
 * @since 25.11.11
 */
public class ObjectRelationship implements Relationship {
    private final ObjectNode start;
    private final RelationshipType relationshipType;
    private final ObjectNode end;
    private ObjectGraphDatabaseService gdb;

    public ObjectRelationship(ObjectNode start, RelationshipType relationshipType, ObjectNode end, ObjectGraphDatabaseService gdb) {
        this.start = start;
        this.relationshipType = relationshipType;
        this.end = end;
        this.gdb = gdb;
    }

    @Override
    public long getId() {
        return start.getId() << 32 + end.getId() + relationshipType.name().hashCode();
    }

    @Override
    public void delete() {

    }

    @Override
    public Node getStartNode() {
        return start;
    }

    @Override
    public Node getEndNode() {
        return end;
    }

    @Override
    public Node getOtherNode(Node node) {
        return node.equals(start) ? end : start;
    }

    @Override
    public Node[] getNodes() {
        return new Node[] {start,end};
    }

    @Override
    public RelationshipType getType() {
        return relationshipType;
    }

    @Override
    public boolean isType(RelationshipType relationshipType) {
        return relationshipType.name().equals(this.relationshipType.name());
    }

    @Override
    public GraphDatabaseService getGraphDatabase() {
        return gdb;
    }

    @Override
    public boolean hasProperty(String name) {
        return false;
    }

    @Override
    public Object getProperty(String s) {
        return null;
    }

    @Override
    public Object getProperty(String s, Object defaultValue) {
        return defaultValue;
    }

    @Override
    public void setProperty(String s, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object removeProperty(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<String> getPropertyKeys() {
        return Collections.emptySet();
    }

    @Override
    public Iterable<Object> getPropertyValues() {
        return Collections.emptySet();
    }
}
