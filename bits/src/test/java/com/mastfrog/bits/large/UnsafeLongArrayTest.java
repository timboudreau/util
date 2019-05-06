package com.mastfrog.bits.large;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Random;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class UnsafeLongArrayTest {

    private final Random rnd = new Random(1384013940139L);

    @Test
    public void testSimple() {
        UnsafeLongArray arr = new UnsafeLongArray(20);
        long[] vals = new long[20];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = rnd.nextLong();
        }
        for (int i = 0; i < vals.length; i++) {
            arr.set(i, vals[i]);
        }
        for (int i = 0; i < vals.length; i++) {
            assertEquals(vals[i], arr.get(i));
        }
    }

    @Test
    public void testFill() {
        UnsafeLongArray arr = new UnsafeLongArray(200);
        for (int i = 0; i < arr.size(); i++) {
            arr.set(i, i);
        }
        for (long i = 0; i < arr.size(); i++) {
            assertEquals(i, arr.get(i));
        }
        long val = Long.MAX_VALUE / 4;
        arr.fill(0, arr.size(), val);
        for (long i = 0; i < arr.size(); i++) {
            assertEquals(i + "", val, arr.get(i));
        }
        long size = arr.size() * 2;
        arr.resize(size);
        for (int i = 0; i < size; i++) {
            arr.set(i, i * 2);
        }
        assertEquals(size, arr.size());
        for (int i = 0; i < size; i++) {
            assertEquals(i * 2, arr.get(i));
        }
        arr.resize(0);
        assertEquals(0, arr.size());

        arr.resize(size);
        assertEquals(size, arr.size());
        for (int i = 0; i < size; i++) {
            arr.set(i, i * 2);
        }

        for (int i = 0; i < size; i++) {
            assertEquals(i * 2, arr.get(i));
        }
        for (int i = 1; i < arr.size(); i++) {
            arr.fill(arr.size() - i, i, 23);
            for (int j = 0; j < arr.size(); j++) {
                if (j < arr.size() - i) {
                    assertEquals("at " + i + "," + j, j * 2, arr.get(j));
                } else {
                    assertEquals("Mismatch at " + i + "," + j, 23, arr.get(j));
                }
            }
        }
    }

    @Test
    public void testGarbageCollection() throws InterruptedException {
        UnsafeLongArray arr = new UnsafeLongArray(Integer.MAX_VALUE / 12);
        long addr = arr.address();
        UnsafeLongArray.Handle handle = arr.handle;
        for (int i = 0; i < arr.size(); i++) {
            arr.set(i, i * 2);
        }
        Reference<UnsafeLongArray> ref = new WeakReference<UnsafeLongArray>(arr);
        arr = null;
        while (ref.get() != null || !handle.isDisposed()) {
            System.gc();
            System.runFinalization();
        }
        assertEquals(addr, UnsafeLongArray.lastCleared);
    }

}
