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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;

/**
 * Placeholder for Joda Time's class of the same name, over java.time. Not
 * complete.
 *
 * @author Tim Boudreau
 */
public interface ReadableInterval {

    boolean abuts(ReadableInterval other);

    Instant end();

    ZonedDateTime endTime();

    ReadableInterval gap(ReadableInterval interval);

    long getEndMillis();

    long getStartMillis();

    boolean isEmpty();

    ReadableInterval overlap(ReadableInterval interval);

    boolean overlaps(ReadableInterval other);

    boolean overlaps(ChronoZonedDateTime when);

    boolean overlaps(Instant instant);

    Instant start();

    ZonedDateTime startTime();

    Duration toDuration();

    long toDurationMillis();

    ReadableInterval toInterval();

    MutableInterval toMutableInterval();

    boolean contains(ReadableInterval other);

    boolean contains(Instant instant);

    boolean isBefore(Instant instant);

    boolean isAfter(Instant instant);

    boolean isBefore(ReadableInterval interval);

    boolean isAfter(ReadableInterval interval);

    Interval withStartMillis(long startInstant);

    Interval withStart(Instant start);

    Interval withEndMillis(long endInstant);

    Interval withEnd(Instant end);
}
