/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.function.state;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
final class IntWithChildrenImpl extends IntImpl implements IntWithChildren {

    private List<Int> children = null;

    IntWithChildrenImpl() {
    }

    IntWithChildrenImpl(int initial) {
        super(initial);
    }

    IntWithChildrenImpl(Int initial) {
        super(initial.getAsInt());
    }

    @Override
    public IntWithChildrenImpl child() {
        IntWithChildrenImpl result = new IntWithChildrenImpl(this);
        if (children == null) {
            children = new LinkedList<>();
        }
        children.add(result);
        return result;
    }

    @Override
    public int increment(int val) {
        int result = super.increment(val);
        if (children != null) {
            children.forEach((kid) -> {
                kid.increment(val);
            });
        }
        return result;
    }
}
