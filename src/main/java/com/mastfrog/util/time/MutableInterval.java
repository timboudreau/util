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

import java.time.Instant;

/**
 *
 * @author Tim Boudreau
 */
public class MutableInterval extends Interval {

    private Instant start;
    private Instant end;

    public MutableInterval(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }
    public MutableInterval(Interval other) {
        this.start = other.start();
        this.end = other.end();
    }
    
    public MutableInterval setStart(Instant start) {
        this.start = start;
        if (start.isAfter(end)) {
            this.start = end;
            this.end = start;
        }
        return this;
    }
    
    public MutableInterval setEnd(Instant end) {
        this.end = end;
        if (end.isAfter(start)) {
            this.end = start;
            this.start = end;
        }
        return this;
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
        return new MutableInterval(this);
    }

}
