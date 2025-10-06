/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ExtractTest {

    /*
     * Testing strategy for Extract:
     *
     * getTimespan():
     * Partition the inputs as follows:
     * - tweets.size(): 0, 1, >1
     * - timestamp ordering: chronological, reverse chronological, mixed order
     * - timestamp equality: all same, all different, some duplicates
     *
     *
     * getMentionedUsers():
     * Partition the inputs as follows:
     * - tweets.size(): 0, 1, >1
     * - mentions per tweet: 0, 1, >1
     * - mention position in text: at start, in middle, at end
     * - mention case: lowercase, uppercase, mixed case
     * - duplicate mentions: none, within same tweet, across different tweets, both cases with different cases
     * - mention validity:
     *     * valid @mention (preceded/followed by space or punctuation)
     *     * invalid: email address (@ preceded by valid username char)
     *     * invalid: @ followed by non-username chars
     * - edge cases: mention with punctuation, multiple mentions in a row, @ at very start/end
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");
    private static final Instant d4 = Instant.parse("2016-02-17T09:00:00Z");
    
    private static final Tweet tweet1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    
    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    /*
     * Tests for getTimespan()
     */

    @Test
    public void testGetTimespanTwoTweets() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet2));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    @Test
    public void testGetTimespanOneTweet() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d1, timespan.getEnd());
    }

    @Test
    public void testGetTimespanTwoTweetsReverseOrder() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet2, tweet1));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    @Test
    public void testGetTimespanMultipleTweetsMixedOrder() {
        Tweet tweet3 = new Tweet(3, "user1", "tweet at noon", d3);
        Tweet tweet4 = new Tweet(4, "user2", "earliest tweet", d4);

        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet3, tweet4, tweet2));

        assertEquals("expected start", d4, timespan.getStart());
        assertEquals("expected end", d3, timespan.getEnd());
    }

    @Test
    public void testGetTimespanAllSameTimestamp() {
        Tweet tweetA = new Tweet(5, "alice", "first", d1);
        Tweet tweetB = new Tweet(6, "bob", "second", d1);
        Tweet tweetC = new Tweet(7, "charlie", "third", d1);

        Timespan timespan = Extract.getTimespan(Arrays.asList(tweetA, tweetB, tweetC));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d1, timespan.getEnd());
    }

    /*
     * Tests for getMentionedUsers()
     */

    @Test
    public void testGetMentionedUsersNoMention() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet1));

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    @Test
    public void testGetMentionedUsersEmptyList() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList());

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    @Test
    public void testGetMentionedUsersOneMention() {
        Tweet tweet = new Tweet(10, "alyssa", "hey @bob how are you?", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        Set<String> lowerCaseUsers = makeLowerCase(mentionedUsers);

        assertEquals("expected one user", 1, mentionedUsers.size());
        assertTrue("expected set to contain bob", lowerCaseUsers.contains("bob"));
    }

    @Test
    public void testGetMentionedUsersMultipleMentionsInOneTweet() {
        Tweet tweet = new Tweet(11, "alyssa", "hey @bob and @charlie, meet @dave", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        Set<String> lowerCaseUsers = makeLowerCase(mentionedUsers);

        assertEquals("expected three users", 3, mentionedUsers.size());
        assertTrue("expected set to contain bob", lowerCaseUsers.contains("bob"));
        assertTrue("expected set to contain charlie", lowerCaseUsers.contains("charlie"));
        assertTrue("expected set to contain dave", lowerCaseUsers.contains("dave"));
    }

    @Test
    public void testGetMentionedUsersCaseInsensitiveDuplicates() {
        Tweet tweet1 = new Tweet(12, "alice", "hey @BOB", d1);
        Tweet tweet2 = new Tweet(13, "ben", "hi @bob", d2);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet1, tweet2));

        assertEquals("expected one unique user despite different cases", 1, mentionedUsers.size());
    }

    @Test
    public void testGetMentionedUsersDuplicateInSameTweet() {
        Tweet tweet = new Tweet(14, "alice", "@bob hello @bob again @BOB", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));

        assertEquals("expected one unique user", 1, mentionedUsers.size());
    }

    @Test
    public void testGetMentionedUsersEmailAddressNotMention() {
        Tweet tweet = new Tweet(15, "alice", "email me at alice@mit.edu please", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));

        assertTrue("expected empty set, email should not be treated as mention",
                mentionedUsers.isEmpty());
    }

    @Test
    public void testGetMentionedUsersMentionAtStart() {
        Tweet tweet = new Tweet(16, "alice", "@bob hey there", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        Set<String> lowerCaseUsers = makeLowerCase(mentionedUsers);

        assertEquals("expected one user", 1, mentionedUsers.size());
        assertTrue("expected set to contain bob", lowerCaseUsers.contains("bob"));
    }

    @Test
    public void testGetMentionedUsersMentionAtEnd() {
        Tweet tweet = new Tweet(17, "alice", "hey there @bob", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        Set<String> lowerCaseUsers = makeLowerCase(mentionedUsers);

        assertEquals("expected one user", 1, mentionedUsers.size());
        assertTrue("expected set to contain bob", lowerCaseUsers.contains("bob"));
    }

    @Test
    public void testGetMentionedUsersWithPunctuation() {
        Tweet tweet = new Tweet(18, "alice", "hey @bob! and @charlie.", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        Set<String> lowerCaseUsers = makeLowerCase(mentionedUsers);

        assertEquals("expected two users", 2, mentionedUsers.size());
        assertTrue("expected set to contain bob", lowerCaseUsers.contains("bob"));
        assertTrue("expected set to contain charlie", lowerCaseUsers.contains("charlie"));
    }

    @Test
    public void testGetMentionedUsersMultipleTweets() {
        Tweet tweet1 = new Tweet(19, "alice", "hey @bob", d1);
        Tweet tweet2 = new Tweet(20, "ben", "@charlie what's up", d2);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet1, tweet2));
        Set<String> lowerCaseUsers = makeLowerCase(mentionedUsers);

        assertEquals("expected two users", 2, mentionedUsers.size());
        assertTrue("expected set to contain bob", lowerCaseUsers.contains("bob"));
        assertTrue("expected set to contain charlie", lowerCaseUsers.contains("charlie"));
    }

    @Test
    public void testGetMentionedUsersValidUsernameChars() {
        Tweet tweet = new Tweet(21, "alice", "hey @bob_smith and @charlie-brown", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        Set<String> lowerCaseUsers = makeLowerCase(mentionedUsers);

        assertEquals("expected two users", 2, mentionedUsers.size());
        assertTrue("expected set to contain bob_smith", lowerCaseUsers.contains("bob_smith"));
        assertTrue("expected set to contain charlie-brown", lowerCaseUsers.contains("charlie-brown"));
    }

    @Test
    public void testGetMentionedUsersMentionWithNumbers() {
        Tweet tweet = new Tweet(22, "alice", "hey @user123 and @bob2", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        Set<String> lowerCaseUsers = makeLowerCase(mentionedUsers);

        assertEquals("expected two users", 2, mentionedUsers.size());
        assertTrue("expected set to contain user123", lowerCaseUsers.contains("user123"));
        assertTrue("expected set to contain bob2", lowerCaseUsers.contains("bob2"));
    }

    @Test
    public void testGetMentionedUsersMixedCaseInTweet() {
        Tweet tweet = new Tweet(23, "alice", "hey @BoB and @CHARLIE", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));
        Set<String> lowerCaseUsers = makeLowerCase(mentionedUsers);

        assertEquals("expected two users", 2, mentionedUsers.size());
        assertTrue("expected set to contain bob", lowerCaseUsers.contains("bob"));
        assertTrue("expected set to contain charlie", lowerCaseUsers.contains("charlie"));
    }

    @Test
    public void testGetMentionedUsersNoSpaceBeforeMention() {
        Tweet tweet = new Tweet(25, "alice", "word@bob should not match", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));

        assertTrue("expected empty set, @ preceded by valid username char",
                mentionedUsers.isEmpty());
    }

    /**
     * Helper method to convert set of strings to lowercase for case-insensitive checking.
     * Needed because the spec is underdetermined about what case to return.
     */

    private static Set<String> makeLowerCase(Set<String> strings) {
        Set<String> result = new HashSet<>();
        for (String s : strings) {
            result.add(s.toLowerCase());
        }
        return result;
    }

}
