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

import com.mastfrog.util.Checks;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Replacement for Joda-Time's Interval class, over JDK 8.
 *
 * @author Tim Boudreau
 */
public abstract class Interval implements ReadableInterval {

    @Override
    public Interval toInterval() {
        return this;
    }

    @Override
    public long getStartMillis() {
        return start().toEpochMilli();
    }

    @Override
    public long getEndMillis() {
        return end().toEpochMilli();
    }

    @Override
    public long toDurationMillis() {
        return getEndMillis() - getStartMillis();
    }

    @Override
    public boolean overlaps(ReadableInterval other) {
        return overlaps(other.start()) || overlaps(other.end());
    }

    @Override
    public boolean abuts(ReadableInterval other) {
        return start().equals(other.end()) || end().equals(other.start());
    }

    @Override
    public Duration toDuration() {
        return Duration.between(start(), end());
    }

    @Override
    public boolean overlaps(ChronoZonedDateTime when) {
        return overlaps(when.toInstant());
    }
    
    @Override
    public boolean contains(Instant instant) {
        return instant.equals(start()) || (instant.isAfter(start()) && instant.isBefore(end()));
    }
    
    @Override
    public boolean isBefore (Instant instant) {
        return end().isBefore(instant);
    }
    
    @Override
    public boolean isAfter(Instant instant) {
        return start().isAfter(instant);
    }
    
    @Override
    public boolean isBefore(ReadableInterval interval) {
        return end().isBefore(interval.start());
    }
    
    @Override
    public boolean isAfter(ReadableInterval interval) {
        return start().isAfter(interval.end());
    }

    @Override
    public ZonedDateTime startTime() {
        long millis = start().toEpochMilli();
        return TimeUtil.fromUnixTimestamp(millis).withZoneSameInstant(ZoneId.systemDefault());
    }

    @Override
    public ZonedDateTime endTime() {
        long millis = end().toEpochMilli();
        return TimeUtil.fromUnixTimestamp(millis).withZoneSameInstant(ZoneId.systemDefault());
    }

    @Override
    public boolean overlaps(Instant instant) {
        Instant start = start();
        Instant end = end();
        return start.equals(instant) || end.equals(instant)
                || (start.isBefore(instant) && end.isAfter(instant));
    }
    
    @Override
    public boolean contains(ReadableInterval other) {
        return other.start().isAfter(start()) && other.end().isBefore(end());
    }

    public static Interval create(ChronoZonedDateTime<?> start, ChronoZonedDateTime<?> end) {
        Checks.notNull("start", start);
        Checks.notNull("end", end);
        return create(start.toInstant(), end.toInstant());
    }
    
    public static Interval create(Duration dur, ChronoZonedDateTime<?> end) {
        ChronoZonedDateTime<?> start = end.minus(dur);
        return create(start, end);
    }
    
    public static Interval create(ChronoZonedDateTime<?> start, Duration dur) {
        ChronoZonedDateTime<?> end = start.plus(dur);
        return create(start, end);
    }
    
    public static Interval create(long startMillisEpoch, long endMillisEpoch) {
        return create(Instant.ofEpochMilli(startMillisEpoch), Instant.ofEpochMilli(endMillisEpoch));
    }

    public static Interval create(Instant start, Instant end) {
        if (end.isBefore(start)) {
            Instant tmp = end;
            end = start;
            start = tmp;
        }
        return new ImmutableInterval(start, end);
    }

    @Override
    public int hashCode() {
        return start().hashCode() + 73 * end().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof Interval) {
            Interval i = (Interval) o;
            return getStartMillis() == i.getStartMillis() && getEndMillis() == i.getEndMillis();
        }
        return false;
    }

    @Override
    public String toString() {
        return TimeUtil.format(toDuration()) + " from " + DateTimeFormatter.ISO_INSTANT.format(start())
                + " to " + DateTimeFormatter.ISO_INSTANT.format(end());
    }

    @Override
    public Interval overlap(ReadableInterval interval) {
        if (overlaps(interval)) {
            Instant sa = start();
            Instant sb = interval.start();
            Instant ea = end();
            Instant eb = interval.end();
            Instant st = sa.isAfter(sb) ? sa : sb;
            Instant en = eb.isAfter(sb) ? ea : sa;
            return create(st, en);
        }
        return null;
    }

    @Override
    public MutableInterval toMutableInterval() {
        return new MutableInterval(start(), end());
    }

    @Override
    public boolean isEmpty() {
        return toDuration().toMillis() == 0;
    }

    @Override
    public Interval gap(ReadableInterval interval) {
        if (overlaps(interval)) {
            Interval ol = overlap(interval);
            return new ImmutableInterval(ol.start(), ol.start());
        }
        Instant sa = start();
        Instant sb = interval.start();
        Instant ea = end();
        Instant eb = interval.end();
        
        Instant en = sa.isAfter(ea) ? sa : sb;
        Instant st = en == sa ? eb : ea;
        return create(st, en);
    }

    @Override
    public Interval withStartMillis(long startInstant) {
        return Interval.create(startInstant, getEndMillis());
    }

    @Override
    public Interval withStart(Instant start) {
        return Interval.create(start, end());
    }

    @Override
    public Interval withEndMillis(long endInstant) {
        return Interval.create(getStartMillis(), endInstant);
    }

    @Override
    public Interval withEnd(Instant end) {
        return Interval.create(start(), end);
    }

    private static final class ImmutableInterval extends Interval {

        private final Instant start;
        private final Instant end;

        public ImmutableInterval(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public Instant start() {
            return start;
        }

        @Override
        public Instant end() {
            return end;
        }

        @Override
        public MutableInterval toMutableInterval() {
            return new MutableInterval(start, end);
        }

    }
}
