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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

public class ThrowingRunnableTest {

    @Test
    public void testRunsAreInReverseAdditionOrder() throws Throwable {
        ThrowingRunnable t = ThrowingRunnable.composable(true);
        List<OrderedR> l = new ArrayList<>(4);
        List<OrderedR> callOrder = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            OrderedR or = new OrderedR(i, callOrder);
            l.add(or);
            ThrowingRunnable old = t;
            t = t.andAlways(or);
            assertSame(old, t);
        }
        t.run();
        assertEquals(l.size(), callOrder.size());
        assertTrue(callOrder.containsAll(l), () -> callOrder.toString());
        for (int i = 0; i < 4; i++) {
            OrderedR or = callOrder.get(i);
            assertEquals(3 - i, or.ix, "Call order not reversed: " + callOrder);
        }
    }

    static final class OrderedR implements ThrowingRunnable {

        private final int ix;
        private final List<OrderedR> callOrder;

        OrderedR(int ix, List<OrderedR> callOrder) {
            this.ix = ix;
            this.callOrder = callOrder;
        }

        public String toString() {
            return Integer.toString(ix);
        }

        @Override
        public void run() throws Exception {
            callOrder.add(this);
        }
    }

    @Test
    public void testComposableAllCalled() throws Throwable {
        ThrowingRunnable t = ThrowingRunnable.composable();
        List<AC> all = new ArrayList<AC>();
        for (int i = 0; i < 20; i++) {
            ThrowingRunnable old = t;
            AC ac;
            if (i % 3 == 2) {
                ac = new RCR(i, new AtomicBoolean());
            } else {
                ac = new RCTR(i, new AtomicBoolean());
            }
            all.add(ac);
            if (ac instanceof ThrowingRunnable) {
                ThrowingRunnable tr = (ThrowingRunnable) ac;
                t = t.andAlways(tr);
            } else {
                Runnable r = (Runnable) ac;
                t = t.andAlwaysRun(r);
            }
            assertSame(old, t, "andAlways of " + ac + " returned different identity");
        }
        for (AC ac : all) {
            ac.assertNotCalled();
        }
        t.run();
        for (AC ac : all) {
            ac.assertCalled();
        }
        for (AC ac : all) {
            ac.assertNotCalled();
        }
        t.run();
        for (AC ac : all) {
            ac.assertCalled();
        }
    }

    @Test
    public void testComposableAllCalledOneShot() throws Throwable {
        ThrowingRunnable t = ThrowingRunnable.oneShot();
        List<AC> all = new ArrayList<AC>();
        for (int i = 0; i < 20; i++) {
            ThrowingRunnable old = t;
            AC ac;
            if (i % 3 == 2) {
                ac = new RCR(i, new AtomicBoolean());
            } else {
                ac = new RCTR(i, new AtomicBoolean());
            }
            all.add(ac);
            if (ac instanceof ThrowingRunnable) {
                ThrowingRunnable tr = (ThrowingRunnable) ac;
                t = t.andAlways(tr);
            } else {
                Runnable r = (Runnable) ac;
                t = t.andAlwaysRun(r);
            }
            assertSame(old, t, "andAlways of " + ac + " returned different identity");
        }
        for (AC ac : all) {
            ac.assertNotCalled();
        }
        t.run();
        for (AC ac : all) {
            ac.assertCalled();
        }
        for (AC ac : all) {
            ac.assertNotCalled();
        }
        t.run();
        for (AC ac : all) {
            ac.assertNotCalled();
        }
    }

    @Test
    public void testComposableAllCalledWithErrors() throws Throwable {
        ThrowingRunnable t = ThrowingRunnable.composable();
        List<AC> all = new ArrayList<AC>();
        for (int i = 0; i < 55; i++) {
            ThrowingRunnable old = t;
            AC ac;
            if (i % 3 == 1) {
                ac = new RCR(i, new AtomicBoolean());
            } else if (i % 5 == 0) {
                if (i % 10 == 0) {
                    ac = new RCRE(i, new AtomicBoolean(), i % 20 == 0);
                } else {
                    ac = new RCTE(i, new AtomicBoolean(), i % 25 == 0);
                }
            } else {
                ac = new RCTR(i, new AtomicBoolean());
            }
            all.add(ac);
            if (ac instanceof ThrowingRunnable) {
                ThrowingRunnable tr = (ThrowingRunnable) ac;
                t = t.andAlways(tr);
            } else {
                Runnable r = (Runnable) ac;
                t = t.andAlwaysRun(r);
            }
            assertSame(old, t, "andAlways of " + ac + " returned different identity");
        }
        for (AC ac : all) {
            ac.assertNotCalled();
        }
        try {
            t.run();
            fail("Something should have been thrown");
        } catch (RE | FooException | XErr re) {
            // ok
        }
        for (AC ac : all) {
            ac.assertCalled();
        }
        for (AC ac : all) {
            ac.assertNotCalled();
        }
        try {
            t.run();
            fail("Something should have been thrown");
        } catch (RE | FooException | XErr re) {
            // ok
        }
        for (AC ac : all) {
            ac.assertCalled();
        }
    }

    @Test
    public void testComposableAllCalledOneShotWithErrors() throws Throwable {
        ThrowingRunnable t = ThrowingRunnable.oneShot();
        List<AC> all = new ArrayList<AC>();
        for (int i = 0; i < 55; i++) {
            ThrowingRunnable old = t;
            AC ac;
            if (i % 3 == 1) {
                ac = new RCR(i, new AtomicBoolean());
            } else if (i % 5 == 0) {
                if (i % 10 == 0) {
                    ac = new RCRE(i, new AtomicBoolean(), i % 20 == 0);
                } else {
                    ac = new RCTE(i, new AtomicBoolean(), i % 25 == 0);
                }
            } else {
                ac = new RCTR(i, new AtomicBoolean());
            }
            all.add(ac);
            if (ac instanceof ThrowingRunnable) {
                ThrowingRunnable tr = (ThrowingRunnable) ac;
                t = t.andAlways(tr);
            } else {
                Runnable r = (Runnable) ac;
                t = t.andAlwaysRun(r);
            }
            assertSame(old, t, "andAlways of " + ac + " returned different identity");
        }
        for (AC ac : all) {
            ac.assertNotCalled();
        }
        try {
            t.run();
            fail("Something should have been thrown");
        } catch (RE | FooException | XErr re) {
            // ok
        }
        for (AC ac : all) {
            ac.assertCalled();
        }
        for (AC ac : all) {
            ac.assertNotCalled();
        }
        t.run();
        for (AC ac : all) {
            ac.assertNotCalled();
        }
    }

    interface AC {

        void assertCalled();

        void assertNotCalled();
    }

    static class RCTR implements ThrowingRunnable, AC {

        private final int ix;

        final AtomicBoolean called;

        RCTR(int ix, AtomicBoolean called) {
            this.ix = ix;
            this.called = called;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "-" + ix;
        }

        @Override
        public void assertNotCalled() {
            assertFalse(called.get(), this + " was called");
        }

        @Override
        public void assertCalled() {
            boolean result = called.compareAndSet(true, false);
            assertTrue(result, this + " was not called");
        }

        @Override
        public void run() throws Exception {
            called.set(true);
        }
    }

    static class RCR implements Runnable, AC {

        final AtomicBoolean called;
        private final int ix;

        RCR(int ix, AtomicBoolean called) {
            this.called = called;
            this.ix = ix;
        }

        public String toString() {
            return getClass().getSimpleName() + "-" + ix;
        }

        @Override
        public void assertNotCalled() {
            assertFalse(called.get());
        }

        public void assertCalled() {
            boolean result = called.compareAndSet(true, false);
            assertTrue(result);
        }

        @Override
        public void run() {
            called.set(true);
        }
    }

    static class RCRE extends RCR {

        private final boolean error;

        public RCRE(int ix, AtomicBoolean called, boolean error) {
            super(ix, called);
            this.error = error;
        }

        @Override
        public void run() {
            super.run();
            if (error) {
                throw new XErr();
            } else {
                throw new RE();
            }
        }
    }

    static final class RCTE extends RCTR {

        final boolean error;

        public RCTE(int ix, AtomicBoolean called, boolean error) {
            super(ix, called);
            this.error = error;
        }

        @Override
        public void run() throws Exception {
            super.run();
            if (error) {
                throw new XErr();
            } else {
                throw new FooException();
            }
        }

    }

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

    static final class XErr extends Error {

    }

    static final class RE extends RuntimeException {

    }
}
