package com.mastfrog.bits.large;

import java.util.Random;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class MappedFileLongArrayTest {

    private final Random rnd = new Random(1384013940139L);

    @Test
    public void testSimple() throws Exception {
        MappedFileLongArray arr = new MappedFileLongArray();
        try {
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
        } finally {
            arr.destroy();
        }
    }

    @Test
    public void testFill() throws Exception {
        MappedFileLongArray arr = new MappedFileLongArray();
        try {
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
            for (int i = 0; i < size; i++) {
                arr.set(i, i * 2);
            }
            assertEquals(size, arr.size());
            for (int i = 0; i < size; i++) {
                assertEquals(i * 2, arr.get(i));
            }
            arr.close();
            arr = new MappedFileLongArray(arr.file());
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
        } finally {
            arr.destroy();
        }
    }

    @Test
    public void testSimpleJava() throws Exception {
        JavaLongArray arr = new JavaLongArray(20);
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
    public void testJavaVisitWithPredicateForward() {
        assertEquals(4, firstZero(new JavaLongArray(new long[]{1, 2, 3, 4, 0, 0, 0, 0})));
        assertEquals(0, firstZero(new JavaLongArray(new long[]{0, 2, 3, 4, 0, 0, 0, 0})));
        assertEquals(1, firstNonZero(new JavaLongArray(new long[]{0, 2, 3, 4, 0, 0, 0, 0})));
        assertEquals(-1, firstZero(new JavaLongArray(new long[]{1, 1, 1, 1, 1, 1, 1})));
        assertEquals(-1, lastZero(new JavaLongArray(new long[]{1, 1, 1, 1, 1, 1, 1})));
        assertEquals(-1, lastZero(new JavaLongArray(new long[0])));
        assertEquals(-1, firstZero(new JavaLongArray(new long[0])));
    }

    @Test
    public void testJavaVisitWithPredicateBackward() {
        assertEquals(7, lastZero(new JavaLongArray(new long[]{1, 2, 3, 4, 0, 0, 0, 0})));
        assertEquals(7, lastZero(new JavaLongArray(new long[]{0, 2, 3, 4, 0, 0, 0, 0})));
        assertEquals(3, lastNonZero(new JavaLongArray(new long[]{1, 2, 3, 4, 0, 0, 0, 0})));
        assertEquals(3, lastNonZero(new JavaLongArray(new long[]{0, 2, 3, 4, 0, 0, 0, 0})));
        assertEquals(-1, lastZero(new JavaLongArray(new long[]{1, 1, 1, 1, 1})));
        assertEquals(-1, lastZero(new JavaLongArray(new long[0])));
        assertEquals(-1, lastNonZero(new JavaLongArray(new long[0])));
    }

    @Test
    public void testFillJava() throws Exception {
        JavaLongArray arr = new JavaLongArray(500);
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

    private static long firstZero(LongArray arr) {
        return arr.firstMatchAscending(val -> {
            return val == 0;
        });
    }

    private static long firstNonZero(LongArray arr) {
        return arr.firstMatchAscending(val -> {
            return val != 0;
        });
    }

    private static long lastZero(LongArray arr) {
        return arr.firstMatchDescending(val -> {
            boolean result = val == 0;
            return result;
        });
    }

    private static long lastNonZero(LongArray arr) {
        return arr.firstMatchDescending(val -> {
            return val != 0;
        });
    }
}
