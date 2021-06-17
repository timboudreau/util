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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/**
 *
 * Since it uses the tail bit to track whether true has been returned for a
 * given timestamp or not, this code will cease working correctly in 292,471,208
 * years.
 *
 * @author Tim Boudreau
 */
public class Periodic implements BooleanSupplier {

    private static final long MASK = 1L << 63;
    private final long interval;
    private AtomicLong lastUpdated = new AtomicLong();

    public Periodic(long millis) {
        this.interval = millis;
    }

    public Periodic(Duration dur) {
        this(dur.toMillis());
    }

    @Override
    public boolean getAsBoolean() {
        long now = System.currentTimeMillis();
        long nue = lastUpdated.updateAndGet(old -> {
            long masked = maskedTimstamp(old);
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

    private static boolean hasBit(long val) {
        return (val & MASK) != 0;
    }

    private static long maskedTimstamp(long val) {
        return val & ~MASK;
    }

    private static long withBit(long val) {
        return val | MASK;
    }

    public static void main(String[] args) {
        long now = System.currentTimeMillis();
        System.out.println("NOW " + now);
        StringBuilder sb = new StringBuilder(Long.toBinaryString(now));
        while (sb.length() < 64) {
            sb.insert(0, 'x');
        }
        System.out.println(sb);

        System.out.println("164 is " + (1L << 63));
        System.out.println("164 is " + Long.toHexString(1L << 63));
        System.out.println("164 is " + Long.toBinaryString(1L << 63));

        System.out.println("");
        Date nowD = new Date(now);
        System.out.println("NOW " + nowD);
        Date mNow = new Date(withBit(now));
        System.out.println("NOW MASKED: " + mNow);

        Duration dur = Duration.between(nowD.toInstant(), mNow.toInstant());

        long mt = withBit(now);
        long diffSecs = (mt - now) / 1000;

        System.out.println("Duration years " + (diffSecs / 31536000L));

        long then = System.currentTimeMillis();

        List<Long> vals = new ArrayList<>(100);
        Periodic p = new Periodic(10);
        while ((now = System.currentTimeMillis()) - then < 1000) {
            if (p.getAsBoolean()) {
                vals.add(now);
            }
        }

        for (int i = 1; i < vals.size(); i++) {
            long curr = vals.get(i);
            long prev = vals.get(i-1);
            System.out.println(i + ".\t" + (curr - prev));
        }
    }
}
