/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.util.time;

import java.time.Duration;
import java.time.ZonedDateTime;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class IntervalTest {

    private final ZonedDateTime now = TimeUtil.fromUnixTimestamp(1_496_217_811_544L);
    private final ZonedDateTime included = now.plus(Duration.ofSeconds(15));
    private final ZonedDateTime future = now.plus(Duration.ofSeconds(30));
    private final ZonedDateTime furtherFuture = now.plus(Duration.ofSeconds(60));

    private final ZonedDateTime tomorrow = now.plus(Duration.ofDays(1));
    private final ZonedDateTime laterTomorrow = tomorrow.plus(Duration.ofHours(2));
    
    @Test
    public void testReversed() {
        Interval a = Interval.create(now, future);
        Interval b = Interval.create(future, now);
        assertEquals(a.start(), b.start());
        assertEquals(a.end(), b.end());
        assertNotEquals(a.start(), a.end());
        assertNotEquals(b.start(), b.end());
        assertEquals(a, b);
        
        a = Interval.create(now.toInstant(), future.toInstant());
        b = Interval.create(future.toInstant(), now.toInstant());
        
        assertEquals(a.start(), b.start());
        assertEquals(a.end(), b.end());
        assertNotEquals(a.start(), a.end());
        assertNotEquals(b.start(), b.end());
        assertEquals(a, b);

        a = Interval.create(now.toInstant().toEpochMilli(), future.toInstant().toEpochMilli());
        b = Interval.create(future.toInstant().toEpochMilli(), now.toInstant().toEpochMilli());
        
        assertEquals(a.start(), b.start());
        assertEquals(a.end(), b.end());
        assertNotEquals(a.start(), a.end());
        assertNotEquals(b.start(), b.end());
        assertEquals(a, b);
    }

    @Test
    public void testOverlap() {
        Interval a = Interval.create(now.toInstant(), future.toInstant());
        assertEquals(Duration.ofSeconds(30), a.toDuration());
        assertEquals(now.toInstant(), a.start());
        assertEquals(future.toInstant(), a.end());
        assertEquals(now.toInstant(), a.start());
        assertEquals(future.toInstant(), a.end());
        assertTrue(a.overlaps(a));
        assertFalse(a.abuts(a));

        assertEquals(now, a.startTime());
        assertEquals(future, a.endTime());

        assertTrue(a.overlaps(included));
        assertFalse(a.overlaps(furtherFuture));

        Interval endsLater = Interval.create(now, furtherFuture);
        assertNotEquals(a, endsLater);
        assertNotSame(a.hashCode(), endsLater.hashCode());

        assertTrue(endsLater.overlaps(a));
        assertTrue(a.overlaps(endsLater));
        assertFalse(a.abuts(endsLater));
        assertFalse(endsLater.abuts(a));

        Interval abutting = Interval.create(future, furtherFuture);
        assertTrue(endsLater.overlaps(abutting));
        assertTrue(a.overlaps(abutting));
        assertTrue(abutting.overlaps(a));
        assertTrue(abutting.overlaps(endsLater));
        assertTrue(a.abuts(abutting));
        assertTrue(abutting.abuts(a));

        Interval unrelated = Interval.create(tomorrow, laterTomorrow);
        assertFalse(a.overlaps(unrelated));
        assertFalse(unrelated.overlaps(a));
        assertFalse(unrelated.abuts(a));
        assertFalse(a.abuts(unrelated));

    }

}
