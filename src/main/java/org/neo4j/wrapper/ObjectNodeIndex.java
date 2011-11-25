package org.neo4j.wrapper;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.IteratorUtil;

import java.util.*;

/**
 * @author mh
 * @since 25.11.11
 */
public class ObjectNodeIndex implements Index<Node> {
    private final String name;
    Map<String,Map<Object,Collection<Node>>> data = new HashMap<String, Map<Object, Collection<Node>>>();

    public ObjectNodeIndex(String name) {
        this.name = name;
    }

    @Override
    public void add(Node node, String property, Object value) {
        if (!data.containsKey(property)) {
            data.put(property, new HashMap<Object, Collection<Node>>());
        }
        final Map<Object, Collection<Node>> values = data.get(property);
        if (!values.containsKey(value)) {
            values.put(value,new HashSet<Node>());
        }
        values.get(value).add(node);
    }

    @Override
    public void remove(Node node, String property, Object value) {
        if (!data.containsKey(property)) return;
        final Map<Object, Collection<Node>> values = data.get(property);
        if (!values.containsKey(value)) return;
        values.remove(node);
        if (values.isEmpty()) data.remove(property);
    }

    @Override
    public void remove(Node node, String property) {
        if (!data.containsKey(property)) return;
        final Map<Object, Collection<Node>> values = data.get(property);
        for (Iterator<Collection<Node>> it = values.values().iterator(); it.hasNext(); ) {
            Collection<Node> nodes = it.next();
            nodes.remove(node);
            if (nodes.isEmpty()) {
                it.remove();
            }
        }
        if (values.isEmpty()) data.remove(property);
    }

    @Override
    public void remove(Node node) {
        for (Iterator<Map<Object, Collection<Node>>> itProperties = data.values().iterator(); itProperties.hasNext(); ) {
            Map<Object, Collection<Node>> values = itProperties.next();
            for (Iterator<Collection<Node>> itValues = values.values().iterator(); itValues.hasNext(); ) {
                Collection<Node> nodes = itValues.next();
                nodes.remove(node);
                if (nodes.isEmpty()) {
                    itValues.remove();
                }
            }
        }
    }

    @Override
    public void delete() {
        data.clear();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<Node> getEntityType() {
        return Node.class;
    }

    @Override
    public IndexHits<Node> get(String property, Object value) {
        final Collection<Node> nodes = indexNodes(property, value);
        return new NodeIndexHits(nodes);
    }

    private Collection<Node> indexNodes(String property, Object value) {
        final Map<Object, Collection<Node>> values = indexValues(property);
        if (!values.containsKey(value)) return Collections.emptyList();
        return values.get(value);
    }

    private Map<Object, Collection<Node>> indexValues(String property) {
        if (!data.containsKey(property)) return Collections.emptyMap();
        return data.get(property);
    }

    @Override
    public IndexHits<Node> query(String property, Object value) {
        throw new UnsupportedOperationException();
        // return new NodeIndexHits(indexNodes(property, value));
    }

    @Override
    public IndexHits<Node> query(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWriteable() {
        return true;
    }

    private static class NodeIndexHits implements IndexHits<Node> {
        final Iterator<Node> iterator;
        private final Collection<Node> nodes;

        public NodeIndexHits(Collection<Node> nodes) {
            this.nodes = nodes;
            iterator = nodes.iterator();
        }

        @Override
        public int size() {
            return nodes.size();
        }

        @Override
        public void close() {
        }

        @Override
        public Node getSingle() {
            return IteratorUtil.singleOrNull(nodes);
        }

        @Override
        public float currentScore() {
            return 0;
        }

        @Override
        public Iterator<Node> iterator() {
            return iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Node next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }
}
