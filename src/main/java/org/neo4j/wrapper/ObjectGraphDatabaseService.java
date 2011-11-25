package org.neo4j.wrapper;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.neo4j.kernel.Traversal;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author mh
 * @since 29.07.11
 */
public class ObjectGraphDatabaseService implements GraphDatabaseService {
    private Object root;
    private Map<Long, Node> allNodes = new HashMap<Long, Node>();
    private final ObjectIndexManager objectIndexManager = new ObjectIndexManager();
    private final MappingContext mappingContext=new MappingContext();

    public ObjectNode createNode(Object value) {
        return new ObjectNode(value, this);
    }

    static class MappingContext {
        static class MappingInfo {
            private final Class<?> type;
            private final Map<String,Field> propertyFields;
            private final Map<String,Field> relationshipFields;

            MappingInfo(Class<?> type) {
                this.type = type;
                this.propertyFields = extractPropertyFields(type);
                this.relationshipFields = extractRelationshipFields(type);
            }
            private Map<String, Field> extractRelationshipFields(Class<?> type) {
                final Field[] allFields = type.getDeclaredFields();
                Map<String, Field> result = new TreeMap<String, Field>();
                for (Field field : allFields) {
                    if (Iterable.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        result.put(field.getName(), field);
                    }
                }
                return Collections.unmodifiableMap(result);
            }
        
            private Map<String, Field> extractPropertyFields(Class<?> type) {
                final Field[] allFields = type.getDeclaredFields();
                Map<String, Field> result = new TreeMap<String, Field>();
                for (Field field : allFields) {
                    if (Iterable.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    result.put(field.getName(), field);
                }
                return Collections.unmodifiableMap(result);
            }

            public Map<String, Field> getPropertyFields() {
                return propertyFields;
            }

            public Map<String, Field> getRelationshipFields() {
                return relationshipFields;
            }

            public Class<?> getType() {
                return type;
            }
        }
        Map<Class,MappingInfo> mappings = new HashMap<Class, MappingInfo>();
        
        public MappingInfo getInfo(Class<?> type) {
            if (!mappings.containsKey(type)) {
                mappings.put(type, new MappingInfo(type));
            }
            return mappings.get(type);
        }
    }
    
    public ObjectGraphDatabaseService(Object root) {
        this.root = root;
        for (Node node : Traversal.description().breadthFirst().traverse(getReferenceNode()).nodes()) {
            allNodes.put(node.getId(), node);
            final ObjectNode objectNode = (ObjectNode) node;
            addToIndex(node, objectNode);
        }
    }

    private void addToIndex(Node node, ObjectNode objectNode) {
        final String indexName = objectNode.getType().getSimpleName();
        final Index<Node> index = index().forNodes(indexName);
        for (String property : node.getPropertyKeys()) {
            index.add(node, property, node.getProperty(property));
        }
    }

    @Override
    public Node createNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectNode getNodeById(long id) {
        return (ObjectNode) allNodes.get(id);
    }

    @Override
    public Relationship getRelationshipById(long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectNode getReferenceNode() {
        return new ObjectNode(root, this);
    }

    @Override
    public Iterable<Node> getAllNodes() {
        return allNodes.values();
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
    }

    @Override
    public Transaction beginTx() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> TransactionEventHandler<T> registerTransactionEventHandler(TransactionEventHandler<T> tTransactionEventHandler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> TransactionEventHandler<T> unregisterTransactionEventHandler(TransactionEventHandler<T> tTransactionEventHandler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public KernelEventHandler registerKernelEventHandler(KernelEventHandler kernelEventHandler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public KernelEventHandler unregisterKernelEventHandler(KernelEventHandler kernelEventHandler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IndexManager index() {
        return objectIndexManager;
    }

    Map<String, Field> getRelationshipFields(Class<?> type) {
        return mappingContext.getInfo(type).getRelationshipFields();
    }

    Map<String, Field> getPropertyFields(Class<?> type) {
        return mappingContext.getInfo(type).getPropertyFields();
    }

    public Iterator<Object> getNodeValues(Iterator<Node> nodes) {
        return new IteratorWrapper<Object, Node>(nodes) {
            @Override
            protected Object underlyingObjectToObject(Node node) {
                return ((ObjectNode) node).getValue();
            }
        };
    }

    public String indexName(Class<?> type) {
        return type.getSimpleName();
    }
}
