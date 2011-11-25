## purpose

Wrap existing, large in memory object networks behind the Neo4j graph database API's to enable:

* fast traversals
* graph algorithms
* cypher queries
* gremlin expressions
* visualization
* server

## usage

    class User {
        private String twid;
        private Collection<Tweet> tweeted = new ArrayList<Tweet>();
        ...
    }

     class Tag {
        private String name;
        private Collection<Tweet> tagged = new HashSet<Tweet>();
        ...
     }

    class Tweet {
        private String id;
        private String text;
        private long date;
        private User tweeted;
        private Collection<Tag> tagged = new HashSet<Tag>();
    }

    user = new User("mesirii");
    neo4j = new Tag("neo");
    graphdb = new Tag("graphdb");
    tweet1 = user.tweet("tweet1", neo4j);
    tweet2 = user.tweet("tweet2", neo4j, graphdb);
    tweet3 = user.tweet("tweet3", graphdb);
    tweet4 = user.tweet("tweet4", graphdb);

    gdb = new ObjectGraphDatabaseService(user);
    allTweets = asList(tweet1, tweet2, tweet3,tweet4);

    ObjectNode tweetNode = gdb.createNode(user);
    Iterable<Relationship> relationships = tweetNode.getRelationships(Direction.OUTGOING, DynamicRelationshipType.withName("tweeted"));
    Iterable<Object> tweets = tweetNode.getEndNodeObjects(relationships);
    assertEquals(allTweets, IteratorUtil.asCollection(tweets));

    String query = "start me=node:User(twid={user})
      match me-[:tweeted]->tweet-[:tagged]->tag
      return count(tweet),tag.name order by tag.name asc";
    ExecutionResult result = new ExecutionEngine(gdb).execute(query, map("user", "mesirii"));


## current state

* mostly readonly, some write operations supported
* cypher, traversals, id-lookup, index-lookups work
* no Relationship-Entity support

## ideas

* createNode(type)
* find getNodeById() via traversal not by storing a map
* InstanceIndex which reflects the actual instances
* Relationships for classes with exactly 2 entity-instance fields
* Superclass support ?
