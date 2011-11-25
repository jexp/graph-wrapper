package org.neo4j.wrapper;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mh
 * @since 25.11.11
 */
public class ObjectIndexManager implements IndexManager {
    private final Map<String,Index<Node>> indexes = new HashMap<String, Index<Node>>();

    @Override
    public boolean existsForNodes(String name) {
        return indexes.containsKey(name);
    }

    @Override
    public Index<Node> forNodes(String name) {
        if (!existsForNodes(name)) {
            indexes.put(name, new ObjectNodeIndex(name));
        }
        return indexes.get(name);
    }

    @Override
    public Index<Node> forNodes(String name, Map<String, String> config) {
        return forNodes(name);
    }

    @Override
    public String[] nodeIndexNames() {
        return indexes.keySet().toArray(new String[indexes.size()]);
    }

    @Override
    public boolean existsForRelationships(String name) {
        return false;
    }

    @Override
    public RelationshipIndex forRelationships(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RelationshipIndex forRelationships(String name, Map<String, String> config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] relationshipIndexNames() {
        return new String[0];
    }

    @Override
    public Map<String, String> getConfiguration(Index<? extends PropertyContainer> index) {
        return Collections.emptyMap();
    }

    @Override
    public String setConfiguration(Index<? extends PropertyContainer> index, String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String removeConfiguration(Index<? extends PropertyContainer> index, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AutoIndexer<Node> getNodeAutoIndexer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RelationshipAutoIndexer getRelationshipAutoIndexer() {
        throw new UnsupportedOperationException();
    }
}
