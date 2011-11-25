package org.neo4j.wrapper;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.IteratorUtil;

import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 25.11.11
 */
public class ObjectGraphWrapperTest {

    private ObjectGraphDatabaseService gdb;
    private User user;
    private Tag neo4j;
    private Tag graphdb;
    private Tweet tweet1;
    private Tweet tweet2;
    private Tweet tweet3;
    private Tweet tweet4;
    private List<Tweet> allTweets;

    static class User {
        private String twid;
        private Collection<Tweet> tweeted = new ArrayList<Tweet>();

        User(String twid) {
            this.twid = twid;
        }

        public Tweet tweet(String text, Tag... tags) {
            Tweet tweet = new Tweet(text,this).tagged(tags);
            tweeted.add(tweet);
            return tweet;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            User user = (User) o;

            return twid.equals(user.twid);
        }

        @Override
        public int hashCode() {
            return twid.hashCode();
        }

        @Override
        public String toString() {
            return "@"+twid;
        }
    }
    static class Tag {
        private String name;
        private Collection<Tweet> tagged = new HashSet<Tweet>();

        public Tag(String name) {
            this.name = name;
        }
        public Tag addTweet(Tweet tweet) {
            tagged.add(tweet);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Tag tag = (Tag) o;

            return name.equals(tag.name);

        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "#"+name;
        }
    }
    static class Tweet {
        private String id;
        private String text;
        private long date;
        private User tweeted;
        private Collection<Tag> tagged = new HashSet<Tag>();

        public Tweet(String text, User user) {
            this.id = UUID.randomUUID().toString();
            this.text = text;
            this.tweeted = user;
            this.date = System.currentTimeMillis();
        }

        public Tweet tagged(Tag...tags) {
            tagged.addAll(asList(tags));
            for (Tag tag : tags) {
                tag.addTweet(this);
            }
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Tweet tweet = (Tweet) o;

            return id.equals(tweet.id);

        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return tweeted +": "+  text +" "+ tagged;
        }
    }

    private void createGraph() {
        user = new User("mesirii");
        neo4j = new Tag("neo");
        graphdb = new Tag("graphdb");
        tweet1 = user.tweet("tweet1", neo4j);
        tweet2 = user.tweet("tweet2", neo4j, graphdb);
        tweet3 = user.tweet("tweet3", graphdb);
        tweet4 = user.tweet("tweet4", graphdb);
    }

    @Before
    public void setUp() throws Exception {
        createGraph();
        gdb = new ObjectGraphDatabaseService(user);
        allTweets = asList(tweet1, tweet2, tweet3,tweet4);
    }

    @Test
    public void testGetReferenceNode() {
        final ObjectNode root = gdb.getReferenceNode();
        assertEquals(System.identityHashCode(user), root.getId());
        assertEquals(user, root.getValue());
    }

    @Test
    public void testGetNodeById() {
        final int id = System.identityHashCode(tweet1);
        final ObjectNode node = gdb.getNodeById(id);
        assertEquals(tweet1, node.getValue());
    }

    @Test
    public void testGetProperty() {
        final Node root = gdb.createNode(user);
        assertEquals(user.twid, root.getProperty("twid"));
    }

    @Test
    public void testGetRelationship() {
        final Node tweetNode = gdb.createNode(tweet1);
        assertEquals(gdb.createNode(user), tweetNode.getSingleRelationship(DynamicRelationshipType.withName("tweeted"), Direction.OUTGOING).getEndNode());
    }

    @Test
    public void testGetRelationshipsByDirection() {
        final ObjectNode tweetNode = gdb.createNode(user);
        final Iterable<Relationship> relationships = tweetNode.getRelationships(Direction.OUTGOING);
        final Iterable<Object> tweets = tweetNode.getEndNodeObjects(relationships);
        assertEquals(allTweets, IteratorUtil.asCollection(tweets));
    }
    @Test
    public void testGetRelationshipsByDirectionAndType() {
        final ObjectNode tweetNode = gdb.createNode(user);
        final Iterable<Relationship> relationships = tweetNode.getRelationships(Direction.OUTGOING, DynamicRelationshipType.withName("tweeted"));
        final Iterable<Object> tweets = tweetNode.getEndNodeObjects(relationships);
        assertEquals(allTweets, IteratorUtil.asCollection(tweets));
    }

    @Test
    public void testSimpleCypherQuery() {
        final String query = "start n=node({user}) return n";
        final ExecutionResult result = new ExecutionEngine(gdb).execute(query, map("user", gdb.createNode(user)));
        Iterator<Object> users = gdb.getNodeValues(result.<Node>columnAs("n"));
        assertEquals(asList(user),IteratorUtil.addToCollection(users, new ArrayList()));
    }
    @Test
    public void testCypherIndexQuery() {
        final String query = "start n=node:User(twid={user}) return n";
        final ExecutionResult result = new ExecutionEngine(gdb).execute(query, map("user", "mesirii"));
        final Iterator<Node> nodes = result.columnAs("n");
        Iterator<Object> users = gdb.getNodeValues(nodes);
        assertEquals(asList(user),IteratorUtil.addToCollection(users, new ArrayList()));
    }
    @Test
    public void testCypherMatchQuery() {
        final String query = "start n=node({user}) match n-[:tweeted]->tweet return tweet";
        final ExecutionResult result = new ExecutionEngine(gdb).execute(query, map("user", gdb.createNode(user)));
        Iterator<Object> tweets = gdb.getNodeValues(result.<Node>columnAs("tweet"));
        assertEquals(allTweets,IteratorUtil.addToCollection(tweets, new ArrayList()));
    }

    @Test
    public void testAddRelationship() {
        final Tweet tweet5 = new Tweet("tweet5", user);
        gdb.createNode(neo4j).createRelationshipTo(gdb.createNode(tweet5), DynamicRelationshipType.withName("tagged"));
        assertTrue("relationship created",neo4j.tagged.contains(tweet5));
    }
    @Test(expected = IllegalArgumentException.class)
    public void testAddWrongRelationshipType() {
        final Tweet tweet5 = new Tweet("tweet5", user);
        gdb.createNode(neo4j).createRelationshipTo(gdb.createNode(tweet5), DynamicRelationshipType.withName("tweeted"));
    }
    @Test(expected = IllegalArgumentException.class)
    public void testAddWrongRelationshipTarget() {
        gdb.createNode(neo4j).createRelationshipTo(gdb.createNode(user), DynamicRelationshipType.withName("tagged"));
    }

    @Test
    public void testCompleyCypherQuery() {
        final String query = "start me=node:User(twid={user}) match me-[:tweeted]->tweet-[:tagged]->tag return count(tweet),tag.name order by tag.name asc";
        final ExecutionResult result = new ExecutionEngine(gdb).execute(query, map("user", "mesirii"));
        int size=0;
        for (Map<String, Object> row : result) {
            if (row.get("tag.name").equals("neo4j")) {
                assertEquals(2,row.get("count(tweet)"));
            }
            if (row.get("tag.name").equals("graphdb")) {
                assertEquals(3,row.get("count(tweet)"));
            }
            size++;
        }
        assertEquals(2,size);
    }

    @Test
    public void testIndexExists() {
        assertTrue(gdb.index().existsForNodes(gdb.indexName(User.class)));
        assertTrue(gdb.index().existsForNodes(gdb.indexName(Tweet.class)));
        assertTrue(gdb.index().existsForNodes(gdb.indexName(Tag.class)));
        assertFalse(gdb.index().existsForNodes(gdb.indexName(Object.class)));
    }

    @Test
    public void testGetIndex() throws Exception {
        final Index<Node> userIndex = gdb.index().forNodes(gdb.indexName(User.class));
        final Node hit = userIndex.get("twid", user.twid).getSingle();
        assertNotNull(hit);
        assertEquals(user,((ObjectNode)hit).getValue());
    }
}
