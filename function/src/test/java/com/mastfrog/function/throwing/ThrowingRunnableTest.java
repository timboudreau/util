/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.function.throwing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ThrowingRunnableTest {

    @Test
    public void testConditional() throws Exception {
        TR tr = new TR();
        TR tr2 = new TR();
        tr.andThen(tr2).run();
        tr.assertRun();
        tr2.assertRun();

        tr.andAlways(tr2).run();
        tr.assertRun();
        tr2.assertRun();

        tr.andThenIf(() -> true, tr2).run();
        tr.assertRun();
        tr2.assertRun();

        tr.andThenIf(() -> false, tr2).run();
        tr.assertRun();
        tr2.assertNotRun();

        tr.andAlwaysIf(() -> false, tr2).run();
        tr.assertRun();
        tr2.assertNotRun();
    }

    @Test
    public void testChaining() throws Exception {
        TR tr = new TR();
        ThrowingRunnable.NO_OP.andThen(tr).run();
        tr.assertRun();
        tr.assertNotRun();

        ThrowIt ti = new ThrowIt();
        assertNotNull(runIt(ti.andThen(tr)));
        ti.assertRun();
        tr.assertNotRun();

        assertNotNull(runIt(tr.andThen(ti)));
        tr.assertRun();
        ti.assertRun();

        assertNotNull(runIt(ti.andAlways(tr)));
        tr.assertRun();
        ti.assertRun();

        ThrowIt2 ti2 = new ThrowIt2();

        Exception ex = runIt(ti.andAlways(ti2));
        ti.assertRun();
        ti2.assertRun();
        assertNotNull(ex);
        assertTrue(ex instanceof FooException, ex::toString);
        assertTrue(ex.getSuppressed().length == 1);
        assertTrue(ex.getSuppressed()[0] instanceof BarException, () -> ex.getSuppressed()[0].toString());
    }

    static final Exception runIt(ThrowingRunnable r) {
        try {
            r.run();
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    static class TR implements ThrowingRunnable {

        boolean wasRun;

        @Override
        public void run() throws Exception {
            wasRun = true;
        }

        public void assertRun() {
            boolean val = wasRun;
            wasRun = false;
            assertTrue(val);
        }

        public void assertNotRun() {
            boolean val = wasRun;
            wasRun = false;
            assertFalse(val);
        }
    }

    static class ThrowIt extends TR {

        @Override
        public void run() throws Exception {
            super.run();
            throw new FooException();
        }
    }

    static class ThrowIt2 extends TR {

        @Override
        public void run() throws Exception {
            super.run();
            throw new BarException();
        }
    }

    static final class FooException extends Exception {

    }

    static final class BarException extends Exception {

    }

}
