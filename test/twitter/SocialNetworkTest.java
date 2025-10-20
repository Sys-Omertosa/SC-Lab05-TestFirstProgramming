/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.*;

import org.junit.Test;

public class SocialNetworkTest {

    /*
     * Testing strategy for SocialNetwork:
     *
     * guessFollowsGraph():
     * Partition the inputs as follows:
     * - tweets.size(): 0, 1, >1
     * - mentions per tweet: 0, 1, >1
     * - number of users: 0, 1, >1
     * - self-mentions: user mentions themselves (should not follow themselves)
     * - duplicate mentions: same user mentioned multiple times by same author
     * - case variations: mentions with different cases
     * - multiple tweets from same user: user mentions different people in different tweets
     *
     * influencers():
     * Partition the inputs as follows:
     * - followsGraph.size(): 0, 1, >1
     * - follower counts: no followers, single follower, multiple followers
     * - ties: users with same follower count
     * - order: verify descending order by follower count
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    /*
     * Tests for guessFollowsGraph()
     */

    @Test
    public void testGuessFollowsGraphEmpty() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(new ArrayList<>());

        assertTrue("expected empty graph", followsGraph.isEmpty());
    }

    @Test
    public void testGuessFollowsGraphNoMentions() {
        Tweet tweet1 = new Tweet(1, "alice", "hello world", d1);
        Tweet tweet2 = new Tweet(2, "bob", "just tweeting", d2);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet1, tweet2));

        // Users who don't mention anyone may or may not be in the graph
        // If they are in the graph, they should have empty follow sets
        if (followsGraph.containsKey("alice")) {
            assertTrue("alice should not follow anyone", followsGraph.get("alice").isEmpty());
        }
        if (followsGraph.containsKey("bob")) {
            assertTrue("bob should not follow anyone", followsGraph.get("bob").isEmpty());
        }
    }

    @Test
    public void testGuessFollowsGraphSingleMention() {
        Tweet tweet = new Tweet(1, "alice", "hey @bob what's up?", d1);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet));

        assertTrue("alice should be in graph", followsGraph.containsKey("alice"));
        Set<String> aliceFollows = followsGraph.get("alice");
        assertEquals("alice should follow 1 person", 1, aliceFollows.size());
        assertTrue("alice should follow bob", containsIgnoreCase(aliceFollows, "bob"));
    }

    @Test
    public void testGuessFollowsGraphMultipleMentions() {
        Tweet tweet = new Tweet(1, "alice", "hey @bob and @charlie, let's meet!", d1);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet));

        assertTrue("alice should be in graph", followsGraph.containsKey("alice"));
        Set<String> aliceFollows = followsGraph.get("alice");
        assertEquals("alice should follow 2 people", 2, aliceFollows.size());
        assertTrue("alice should follow bob", containsIgnoreCase(aliceFollows, "bob"));
        assertTrue("alice should follow charlie", containsIgnoreCase(aliceFollows, "charlie"));
    }

    @Test
    public void testGuessFollowsGraphMultipleTweetsFromOneUser() {
        Tweet tweet1 = new Tweet(1, "alice", "hey @bob", d1);
        Tweet tweet2 = new Tweet(2, "alice", "hi @charlie", d2);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet1, tweet2));

        assertTrue("alice should be in graph", followsGraph.containsKey("alice"));
        Set<String> aliceFollows = followsGraph.get("alice");
        assertEquals("alice should follow 2 people", 2, aliceFollows.size());
        assertTrue("alice should follow bob", containsIgnoreCase(aliceFollows, "bob"));
        assertTrue("alice should follow charlie", containsIgnoreCase(aliceFollows, "charlie"));
    }

    @Test
    public void testGuessFollowsGraphDuplicateMentions() {
        Tweet tweet = new Tweet(1, "alice", "@bob hey @bob what's up @BOB?", d1);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet));

        assertTrue("alice should be in graph", followsGraph.containsKey("alice"));
        Set<String> aliceFollows = followsGraph.get("alice");
        assertEquals("alice should follow bob only once", 1, aliceFollows.size());
        assertTrue("alice should follow bob", containsIgnoreCase(aliceFollows, "bob"));
    }

    @Test
    public void testGuessFollowsGraphCaseInsensitive() {
        Tweet tweet1 = new Tweet(1, "alice", "hey @BOB", d1);
        Tweet tweet2 = new Tweet(2, "ALICE", "hi @bob", d2);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet1, tweet2));

        // Should have one key for alice (case-insensitive)
        int aliceCount = 0;
        for (String key : followsGraph.keySet()) {
            if (key.equalsIgnoreCase("alice")) {
                aliceCount++;
            }
        }
        assertEquals("alice should appear once in graph (case-insensitive)", 1, aliceCount);
    }

    @Test
    public void testGuessFollowsGraphNoSelfFollow() {
        Tweet tweet = new Tweet(1, "alice", "I am @alice", d1);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet));

        // If alice is in the graph, she should not follow herself
        if (followsGraph.containsKey("alice")) {
            Set<String> aliceFollows = followsGraph.get("alice");
            assertFalse("alice should not follow herself", containsIgnoreCase(aliceFollows, "alice"));
        }
    }

    @Test
    public void testGuessFollowsGraphMultipleUsers() {
        Tweet tweet1 = new Tweet(1, "alice", "hey @bob", d1);
        Tweet tweet2 = new Tweet(2, "bob", "hi @charlie", d2);
        Tweet tweet3 = new Tweet(3, "charlie", "hello @alice", d3);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(Arrays.asList(tweet1, tweet2, tweet3));

        assertTrue("alice should be in graph", containsKeyIgnoreCase(followsGraph, "alice"));
        assertTrue("bob should be in graph", containsKeyIgnoreCase(followsGraph, "bob"));
        assertTrue("charlie should be in graph", containsKeyIgnoreCase(followsGraph, "charlie"));

        // Verify follow relationships
        String aliceKey = getKeyIgnoreCase(followsGraph, "alice");
        String bobKey = getKeyIgnoreCase(followsGraph, "bob");
        String charlieKey = getKeyIgnoreCase(followsGraph, "charlie");

        assertTrue("alice follows bob", containsIgnoreCase(followsGraph.get(aliceKey), "bob"));
        assertTrue("bob follows charlie", containsIgnoreCase(followsGraph.get(bobKey), "charlie"));
        assertTrue("charlie follows alice", containsIgnoreCase(followsGraph.get(charlieKey), "alice"));
    }

    /*
     * Tests for influencers()
     */

    @Test
    public void testInfluencersEmpty() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertTrue("expected empty list", influencers.isEmpty());
    }

    @Test
    public void testInfluencersSingleUserNoFollowers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", new HashSet<>());

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals("expected one user", 1, influencers.size());
        assertTrue("list should contain alice", containsIgnoreCase(influencers, "alice"));
    }

    @Test
    public void testInfluencersSingleInfluencer() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", new HashSet<>(Arrays.asList("bob")));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        // bob has 1 follower (alice), alice has 0 followers
        assertEquals("expected two users", 2, influencers.size());
        // bob should be first (most influential)
        assertTrue("bob should be more influential than alice",
                influencers.indexOf(getIgnoreCase(influencers, "bob")) <
                influencers.indexOf(getIgnoreCase(influencers, "alice")));
    }

    @Test
    public void testInfluencersMultipleInfluencers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", new HashSet<>(Arrays.asList("bob")));
        followsGraph.put("charlie", new HashSet<>(Arrays.asList("bob")));
        followsGraph.put("dave", new HashSet<>(Arrays.asList("charlie")));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        // bob: 2 followers (alice, charlie)
        // charlie: 1 follower (dave)
        // alice: 0 followers
        // dave: 0 followers

        assertEquals("expected four users", 4, influencers.size());

        int bobIndex = influencers.indexOf(getIgnoreCase(influencers, "bob"));
        int charlieIndex = influencers.indexOf(getIgnoreCase(influencers, "charlie"));
        int aliceIndex = influencers.indexOf(getIgnoreCase(influencers, "alice"));
        int daveIndex = influencers.indexOf(getIgnoreCase(influencers, "dave"));

        assertTrue("bob should be first", bobIndex == 0);
        assertTrue("charlie should be second", charlieIndex == 1);
        // alice and dave both have 0 followers, order between them doesn't matter
        assertTrue("alice and dave should be after charlie", aliceIndex > charlieIndex && daveIndex > charlieIndex);
    }

    @Test
    public void testInfluencersTiedInfluence() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alice", new HashSet<>(Arrays.asList("bob")));
        followsGraph.put("charlie", new HashSet<>(Arrays.asList("dave")));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        // bob: 1 follower (alice)
        // dave: 1 follower (charlie)
        // alice: 0 followers
        // charlie: 0 followers

        assertEquals("expected four users", 4, influencers.size());

        // bob and dave are tied at 1 follower each, should both come before alice and charlie
        int bobIndex = influencers.indexOf(getIgnoreCase(influencers, "bob"));
        int daveIndex = influencers.indexOf(getIgnoreCase(influencers, "dave"));
        int aliceIndex = influencers.indexOf(getIgnoreCase(influencers, "alice"));
        int charlieIndex = influencers.indexOf(getIgnoreCase(influencers, "charlie"));

        assertTrue("bob should come before alice", bobIndex < aliceIndex);
        assertTrue("bob should come before charlie", bobIndex < charlieIndex);
        assertTrue("dave should come before alice", daveIndex < aliceIndex);
        assertTrue("dave should come before charlie", daveIndex < charlieIndex);
    }

    @Test
    public void testInfluencersDescendingOrder() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        // Create a clear hierarchy
        followsGraph.put("user1", new HashSet<>(Arrays.asList("popular")));
        followsGraph.put("user2", new HashSet<>(Arrays.asList("popular")));
        followsGraph.put("user3", new HashSet<>(Arrays.asList("popular", "medium")));
        followsGraph.put("user4", new HashSet<>(Arrays.asList("medium")));

        List<String> influencers = SocialNetwork.influencers(followsGraph);
        // popular: 3 followers
        // medium: 2 followers
        // others: 0 followers
        int popularIndex = influencers.indexOf(getIgnoreCase(influencers, "popular"));
        int mediumIndex = influencers.indexOf(getIgnoreCase(influencers, "medium"));

        assertTrue("popular should be first", popularIndex == 0);
        assertTrue("medium should be second", mediumIndex == 1);
    }

    /*
     * Helper methods for case-insensitive checking
     */

    private static boolean containsIgnoreCase(Set<String> set, String value) {
        for (String item : set) {
            if (item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(List<String> list, String value) {
        for (String item : list) {
            if (item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsKeyIgnoreCase(Map<String, Set<String>> map, String key) {
        for (String k : map.keySet()) {
            if (k.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private static String getKeyIgnoreCase(Map<String, Set<String>> map, String key) {
        for (String k : map.keySet()) {
            if (k.equalsIgnoreCase(key)) {
                return k;
            }
        }
        return null;
    }

    private static String getIgnoreCase(List<String> list, String value) {
        for (String item : list) {
            if (item.equalsIgnoreCase(value)) {
                return item;
            }
        }
        return null;
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * SocialNetwork class that follows the spec. It will be run against several
     * staff implementations of SocialNetwork, which will be done by overwriting
     * (temporarily) your version of SocialNetwork with the staff's version.
     * DO NOT strengthen the spec of SocialNetwork or its methods.
     *
     * In particular, your test cases must not call helper methods of your own
     * that you have put in SocialNetwork, because that means you're testing a
     * stronger spec than SocialNetwork says. If you need such helper methods,
     * define them in a different class. If you only need them in this test
     * class, then keep them in this test class.
     */

}