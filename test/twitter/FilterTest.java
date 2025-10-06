package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FilterTest {

    /*
     * Testing strategy:
     *
     * writtenBy():
     *  - no tweets in input list
     *  - no tweets by username
     *  - some tweets by username
     *  - all tweets by username
     *  - case-insensitive matching
     *
     * inTimespan():
     *  - no tweets in input list
     *  - timespan excludes all tweets
     *  - timespan includes some tweets
     *  - timespan includes all tweets
     *  - tweets exactly at start or end boundary
     *
     * containing():
     *  - no tweets in input list
     *  - empty words list
     *  - no tweets containing words
     *  - some tweets containing words
     *  - all tweets containing words
     *  - case-insensitive word matching
     *  - multiple words searched
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");

    private static final Tweet tweet1 = new Tweet(1, "alyssa", "Talk about rivest", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    private static final Tweet tweet3 = new Tweet(3, "Alyssa", "Another tweet", d3);

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // VM arg: -ea
    }

    // writtenBy tests
    @Test
    public void testWrittenByNone() {
        List<Tweet> result = Filter.writtenBy(Collections.emptyList(), "alyssa");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testWrittenByNoMatch() {
        List<Tweet> result = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "charlie");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testWrittenBySome() {
        List<Tweet> result = Filter.writtenBy(Arrays.asList(tweet1, tweet2, tweet3), "alyssa");
        assertEquals(2, result.size());
        assertTrue(result.contains(tweet1));
        assertTrue(result.contains(tweet3)); // case-insensitive
    }

    @Test
    public void testWrittenByAll() {
        List<Tweet> result = Filter.writtenBy(Arrays.asList(tweet1, tweet3), "alyssa");
        assertEquals(2, result.size());
    }

    // inTimespan tests
    @Test
    public void testInTimespanEmptyList() {
        Timespan span = new Timespan(d1, d3);
        assertTrue(Filter.inTimespan(Collections.emptyList(), span).isEmpty());
    }

    @Test
    public void testInTimespanNoTweetsInside() {
        Timespan span = new Timespan(Instant.parse("2016-02-17T13:00:00Z"), Instant.parse("2016-02-17T14:00:00Z"));
        assertTrue(Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3), span).isEmpty());
    }

    @Test
    public void testInTimespanSomeTweetsInside() {
        Timespan span = new Timespan(d1, d2);
        List<Tweet> result = Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3), span);
        assertEquals(2, result.size());
        assertTrue(result.contains(tweet1));
        assertTrue(result.contains(tweet2));
    }

    @Test
    public void testInTimespanBoundaryIncluded() {
        Timespan span = new Timespan(d1, d3);
        List<Tweet> result = Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3), span);
        assertEquals(3, result.size());
    }

    // containing tests
    @Test
    public void testContainingEmptyList() {
        List<Tweet> result = Filter.containing(Collections.emptyList(), Arrays.asList("talk"));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testContainingEmptyWords() {
        List<Tweet> result = Filter.containing(Arrays.asList(tweet1, tweet2), Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testContainingNoMatch() {
        List<Tweet> result = Filter.containing(Arrays.asList(tweet1, tweet2), Arrays.asList("xyz"));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testContainingSomeMatch() {
        List<Tweet> result = Filter.containing(Arrays.asList(tweet1, tweet2), Arrays.asList("minutes"));
        assertEquals(1, result.size());
        assertTrue(result.contains(tweet2));
    }

    @Test
    public void testContainingAllMatchCaseInsensitive() {
        List<Tweet> result = Filter.containing(Arrays.asList(tweet1, tweet2), Arrays.asList("TALK"));
        assertEquals(2, result.size());
    }

    @Test
    public void testContainingMultipleWords() {
        List<Tweet> result = Filter.containing(Arrays.asList(tweet1, tweet2), Arrays.asList("rivest", "minutes"));
        assertEquals(2, result.size());
    }
}
