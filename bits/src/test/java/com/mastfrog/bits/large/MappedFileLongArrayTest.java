package com.mastfrog.bits.large;

import static java.lang.ProcessBuilder.Redirect.to;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.PrimitiveIterator;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class MappedFileLongArrayTest {

    private final Random rnd = new Random(1384013940139L);

    @Test
    public void testFromArray() throws Exception {
        Random rnd = new Random(61309103931L);
        long[] lngs = new long[2048];
        for (int i = 0; i < lngs.length; i++) {
            lngs[i] = rnd.nextLong();
        }
        Path fa = Paths.get(System.getProperty("java.io.tmpdir")).resolve("tfa-" + System.currentTimeMillis()
                + "-" + rnd.nextInt());
        try {
            MappedFileLongArray.saveForMapping(lngs, fa);
            assertTrue(Files.exists(fa));
            MappedFileLongArray la = new MappedFileLongArray(fa, -1, true);
            assertEquals(lngs.length, la.size());
            long[] read = la.toLongArray();
            assertArrayEquals(lngs, read);
        } finally {
            if (Files.exists(fa)) {
                Files.delete(fa);
            }
        }
    }

    @Test
    public void testFromIter() throws Exception {
        Random rnd = new Random(61309103931L);
        long[] lngs = new long[2048];
        for (int i = 0; i < lngs.length; i++) {
            lngs[i] = rnd.nextLong();
        }

        class P implements PrimitiveIterator.OfLong {

            private int cursor = 0;

            @Override
            public long nextLong() {
                return lngs[cursor++];
            }

            @Override
            public boolean hasNext() {
                return cursor < lngs.length;
            }
        }

        Path fa = Paths.get(System.getProperty("java.io.tmpdir")).resolve("tfa-" + System.currentTimeMillis()
                + "-" + rnd.nextInt());
        try {
            MappedFileLongArray.saveForMapping(new P(), fa);
            assertTrue(Files.exists(fa));
            MappedFileLongArray la = new MappedFileLongArray(fa, -1, true);
            assertEquals(lngs.length, la.size());
            long[] read = la.toLongArray();
            assertArrayEquals(lngs, read);
        } finally {
            if (Files.exists(fa)) {
                Files.delete(fa);
            }
        }
    }

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
                assertEquals(val, arr.get(i), i + "");
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
                        assertEquals(j * 2, arr.get(j), "at " + i + "," + j);
                    } else {
                        assertEquals(23, arr.get(j), "Mismatch at " + i + "," + j);
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
            assertEquals(val, arr.get(i), i + "");
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
                    assertEquals(j * 2, arr.get(j), "at " + i + "," + j);
                } else {
                    assertEquals(23, arr.get(j), "Mismatch at " + i + "," + j);
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
