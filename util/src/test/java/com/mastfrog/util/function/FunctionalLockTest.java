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
package com.mastfrog.util.function;

import com.mastfrog.util.thread.NonThrowingAutoCloseable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class FunctionalLockTest {

    @Test(timeout = 10000)
    public void testFunctionalLock() throws Exception {
        FunctionalLock l = new FunctionalLock();
        assertFalse(l.isReadAccessOnCurrentThread());
        CheckerIt check = new CheckerIt();
        try {
            l.underReadLock(() -> {
                check.run();
                assertTrue(l.isReadAccessOnCurrentThread());
                assertEquals(1, l.getReadLockCount());
                throw new Exception();
            });
            fail("Exception should have been thrown");
        } catch (Exception ex) {
        }
        check.assertRun();
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isWriteLocked());
        assertFalse(l.isReadAccessOnCurrentThread());
        try {
            l.underWriteLock(() -> {
                check.run();
                assertFalse(l.isReadAccessOnCurrentThread());
                assertTrue(l.isWriteLocked());
                assertTrue(l.isWriteLockedByCurrentThread());
                throw new Exception();
            });
            fail("Exception should have been thrown");
        } catch (Exception ex) {
        }
        check.assertRun();
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isWriteLocked());
        assertFalse(l.isReadAccessOnCurrentThread());
        assertEquals("hello", l.<String>supplyUnderReadLock(() -> {
            check.run();
            assertTrue(l.isReadAccessOnCurrentThread());
            assertEquals(1, l.getReadLockCount());
            return "hello";
        }
        ));
        check.assertRun();
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isWriteLocked());
        assertFalse(l.isReadAccessOnCurrentThread());
        assertEquals("hello", l.<String>getUnderWriteLock(() -> {
            check.run();
            assertFalse(l.isReadAccessOnCurrentThread());
            assertEquals(0, l.getReadLockCount());
            assertTrue(l.isWriteLocked());
            assertTrue(l.isWriteLockedByCurrentThread());
            return "hello";
        }
        ));

        check.assertRun();
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isWriteLocked());
        assertFalse(l.isReadAccessOnCurrentThread());
        try {
            l.<String>supplyUnderReadLock(() -> {
                check.run();
                assertTrue(l.isReadAccessOnCurrentThread());
                throw new Exception();
            });
            fail("Exception should have been thrown");
        } catch (Exception e) {
        }
        check.assertRun();
        assertFalse(l.isReadAccessOnCurrentThread());
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isWriteLocked());
        assertEquals("hello", l.<String>supplyUnderWriteLock(() -> {
            check.run();
            assertEquals(true, l.isWriteLocked());
            assertFalse(l.isReadAccessOnCurrentThread());
            assertEquals(true, l.isWriteLockedByCurrentThread());
            return "hello";
        }));
        check.assertRun();
        assertFalse(l.isReadAccessOnCurrentThread());
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isWriteLocked());
        try {
            l.runUnderReadLock(() -> {
                check.run();
                assertTrue(l.isReadAccessOnCurrentThread());
                assertEquals(1, l.getReadLockCount());
                l.runUnderReadLock(() -> {
                    assertTrue(l.isReadAccessOnCurrentThread());
                    assertEquals(2, l.getReadLockCount());
                    throw new RuntimeException();
                });
                assertTrue(l.isReadAccessOnCurrentThread());
            });
            fail("Exception should have been thrown");
        } catch (Exception e) {
        }
        check.assertRun();
        assertFalse(l.isReadAccessOnCurrentThread());
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isWriteLocked());
        try {
            l.runUnderWriteLock(() -> {
                check.run();
                assertFalse(l.isReadAccessOnCurrentThread());
                assertTrue(l.isWriteLocked());
                assertTrue(l.isWriteLockedByCurrentThread());
                throw new RuntimeException();
            });
            fail("Exception should have been thrown");
        } catch (Exception e) {
        }
        check.assertRun();
        assertFalse(l.isReadAccessOnCurrentThread());
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isWriteLocked());
        try (NonThrowingAutoCloseable nte = l.withReadLock()) {
            assertTrue(l.isReadAccessOnCurrentThread());
            assertEquals(1, l.getReadLockCount());
        }
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isReadAccessOnCurrentThread());
        assertFalse(l.isWriteLocked());
        Exception ex = null;
        try (NonThrowingAutoCloseable nte = l.withWriteLock()) {
            check.run();
            assertTrue(l.isWriteLocked());
            assertFalse(l.isReadAccessOnCurrentThread());
            assertTrue(l.isWriteLockedByCurrentThread());
            throw new Exception();
        } catch (Exception e) {
            ex = e;
        }
        assertNotNull(ex);
        check.assertRun();
        assertFalse(l.isReadAccessOnCurrentThread());
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isWriteLocked());
        l.runUnderWriteLock(() -> {
            l.runUnderReadLock(() -> {
                check.run();
                assertEquals(1, l.getReadLockCount());
                assertTrue(l.isReadAccessOnCurrentThread());
                assertEquals(true, l.isWriteLocked());
                assertEquals(true, l.isWriteLockedByCurrentThread());
            });
            assertFalse(l.isReadAccessOnCurrentThread());
        });
        check.assertRun();
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isWriteLocked());
        assertFalse(l.isReadAccessOnCurrentThread());
        ex = null;
        try {
            l.runUnderReadLock(() -> {
                assertTrue(l.isReadAccessOnCurrentThread());
                l.runUnderWriteLock(() -> {
                    check.run();
                    assertTrue(l.isReadAccessOnCurrentThread());
                    assertEquals(1, l.getReadLockCount());
                    assertEquals(true, l.isWriteLocked());
                    assertEquals(true, l.isWriteLockedByCurrentThread());
                });
            });
            fail("Exception should have been thrown");
        } catch (IllegalThreadStateException ex2) {
            ex = ex2;
        }
        assertNotNull(ex);
        ex = null;
        try (NonThrowingAutoCloseable a = l.withReadLock()) {
            check.run();
            assertNotNull(a);
            assertTrue(l.isReadAccessOnCurrentThread());
            try (NonThrowingAutoCloseable b = l.withWriteLock()) {
                fail("Should not get here");
            } catch (IllegalThreadStateException ex1) {
                ex = ex1;
            }
        }
        assertNotNull(ex);
        check.assertRun();
        assertEquals(0, l.getReadLockCount());
        assertFalse(l.isWriteLocked());
        assertFalse(l.isReadAccessOnCurrentThread());
    }

    static final class CheckerIt implements Runnable {

        private boolean run;

        @Override
        public void run() {
            run = true;
        }

        void assertRun() {
            boolean old = run;
            run = false;
            assertTrue(old);
        }

    }

}
