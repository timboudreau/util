/*
 * The MIT License
 *
 * Copyright 2023 Mastfrog Technologies.
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

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class FlipFlopTest {

    @Test
    public void testFlipFlop() {
        Thing thingOne = new Thing(1);
        Thing thingTwo = new Thing(2);
        FlipFlop<Thing> ff = new FlipFlop<>(thingOne, thingTwo, Thing::clear);
        
        assertSame(thingOne, ff.get());
        
        List<Integer> expectOne = new ArrayList<>();
        List<Integer> expectTwo = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            ff.get().add(i, expectOne);
        }
        
        Thing gotOne = ff.flip();
        assertSame(thingOne, gotOne);
        
        for (int i = 10; i < 20; i++) {
            ff.get().add(i, expectTwo);
        }
        
        Thing gotTwo = ff.flip();
        assertSame(thingTwo, gotTwo);
        
        assertTrue(thingOne.isEmpty());
    }

    static class Thing {

        private final int index;
        private List<Integer> stuff = new ArrayList<>();

        public Thing(int index) {
            this.index = index;
        }
        
        boolean isEmpty() {
            return stuff.isEmpty();
        }

        void add(int val, List<Integer> alsoAddTo) {
            stuff.add(val);
            alsoAddTo.add(val);
            assertContents(alsoAddTo);
        }

        public void clear() {
            stuff.clear();
        }

        @Override
        public String toString() {
            return "Thing-" + index + "(" + stuff + ")";
        }

        public void assertContents(List<Integer> expected) {
            assertEquals(stuff, expected, () -> this.toString() + " vs " + expected);
        }

    }
}
