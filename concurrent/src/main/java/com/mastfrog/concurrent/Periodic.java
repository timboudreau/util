/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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
package com.mastfrog.concurrent;

import static java.lang.System.currentTimeMillis;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/**
 * Divides time into a series of epochs of a specific duration, and will return
 * true <i>exactly once</i> per epoch. This is useful for concurrent code that
 * does things like log metrics once every <code>n</code> seconds, where threads
 * could race on the test of whether to log or not, and should not use a lock.
 * <p>
 * Since it uses the tail bit to track whether true has been returned for a
 * given timestamp or not, this code will cease working correctly in 292,471,208
 * years.
 * </p>
 *
 * @author Tim Boudreau
 */
public final class Periodic implements BooleanSupplier {

    private static final long MASK = 1L << 63;
    private final long interval;
    private final AtomicLong lastUpdated;

    public Periodic(long millis) {
        this.lastUpdated = new AtomicLong(maskedTimestamp(currentTimeMillis()));
        this.interval = millis;
    }

    public Periodic(Duration dur) {
        this(dur.toMillis());
    }

    /**
     * Returns true if and <i>only if</i> this instance has not yet returned
     * true during the current epoch (as computed at method entry).
     *
     * @return true if this instance has not previously returned true since the
     * start of this epoch
     */
    @Override
    public boolean getAsBoolean() {
        long now = currentTimeMillis();
        long nue = lastUpdated.updateAndGet(old -> {
            long masked = maskedTimestamp(old);
            if (hasBit(old)) {
                return masked;
            }
            if (now - masked > interval) {
                return withBit(now);
            }
            return old;
        });
        return hasBit(nue);
    }

    /**
     * Run the passed runnable if getAsBoolean() returns <code>true</code>.
     *
     * @param run A runnable
     * @return whether or not the runnable was run
     */
    public boolean maybeExecute(Runnable run) {
        boolean result;
        if (result = getAsBoolean()) {
            run.run();
        }
        return result;
    }

    private static boolean hasBit(long val) {
        return (val & MASK) != 0;
    }

    private static long maskedTimestamp(long val) {
        return val & ~MASK;
    }

    private static long withBit(long val) {
        return val | MASK;
    }

    long ts() {
        return maskedTimestamp(lastUpdated.get());
    }
}
