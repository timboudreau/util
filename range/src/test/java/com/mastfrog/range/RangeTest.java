package com.mastfrog.range;

import static com.mastfrog.range.RangeHolder.checkStartAndSize;
import static com.mastfrog.range.RangeRelation.STRADDLES_END;
import static com.mastfrog.range.RangeRelation.STRADDLES_START;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 *
 * @author Tim Boudreau
 */
@SuppressWarnings("NonPublicExported")
public class RangeTest {

    RangeFactory<?> factory;

    Range<?> of(int start, int size) {
        return factory.range(start, size);
    }

    Range<?> ofCoordinates(int start, int end) {
        return factory.ofCoordinates(start, end);
    }

    int start(Range<?> range) {
        return factory.startOf(range);
    }

    int size(Range<?> range) {
        return factory.sizeOf(range);
    }

    @ParameterizedTest(name = "{index}: {0} - {1}")
    @MethodSource("factories")
    public void testGap(RangeFactory<?> factory) {
        Range<?> a = factory.range(5, 10);
        Range<?> b = factory.range(40, 10);
        Range<?> gap = a.gap(b);
        assertNotNull(gap);
        assertEquals(25, gap.sizeValue().intValue());
        assertEquals(15, gap.startValue().intValue());

        gap = b.gap(a);
        assertNotNull(gap);
        assertEquals(25, gap.sizeValue().intValue());
        assertEquals(15, gap.startValue().intValue());

        gap = a.gap(a);
        assertNotNull(gap);
        assertEquals(0, gap.sizeValue().intValue());
        assertEquals(5, gap.startValue().intValue());
    }

    @ParameterizedTest(name = "{index}: {0} - {1}")
    @MethodSource("factories")
    public void testRelations(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> a = of(0, 10);
        Range<?> b = of(10, 10);
        assertRelation(RangeRelation.BEFORE, a, b);
        assertRelation(RangeRelation.AFTER, b, a);
        Range<?> c = of(5, 10);
        assertRelation(RangeRelation.STRADDLES_START, a, c);
        assertRelation(RangeRelation.STRADDLES_END, b, c);

        Range<?> d = of(4, 11);
        assertRelation(RangeRelation.CONTAINS, d, c);
        assertRelation(RangeRelation.CONTAINED, c, d);

        Range<?> e = of(4, 10);
        assertRelation(RangeRelation.STRADDLES_START, e, c);

        Range<?> f = of(5, 10);
        assertRelation(RangeRelation.EQUAL, f, c);

        Range<?> g = of(10, 11);
        assertRelation(RangeRelation.AFTER, g, a);
        assertRelation(RangeRelation.BEFORE, a, g);

        Range<?> h = of(9, 1);
        assertRelation(RangeRelation.CONTAINED, h, a);
        assertRelation(RangeRelation.CONTAINS, a, h);
        assertRelation(RangeRelation.BEFORE, h, b);
        assertRelation(RangeRelation.AFTER, b, h);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    public void testSorting(RangeFactory<?> factory) {
        this.factory = factory;
        List<Range<?>> ranges = new ArrayList<>();
        ranges.add(of(50, 10));
        ranges.add(of(40, 10));
        ranges.add(of(30, 10));
        ranges.add(of(20, 10));
        ranges.add(of(10, 10));
        ranges.add(of(0, 10));
        Collections.sort(ranges);
        for (int i = 0; i < 6; i++) {
            Range<?> rng = ranges.get(i);
            assertEquals(10 * i, start(rng));
            if (i > 0) {
                assertTrue(rng.abuts(ranges.get(i - 1)));
                assertTrue(ranges.get(i - 1).abuts(rng));
            }
        }
        ranges.add(of(9, 19));
        Collections.sort(ranges);
        assertEquals(9, start(ranges.get(1)));
        ranges.add(of(9, 20));
        Collections.sort(ranges);
        assertEquals(9, start(ranges.get(1)));
        assertEquals(20, size(ranges.get(1)));
    }

    @ParameterizedTest(name = "woohoo {1}")
    @MethodSource("factories")
    public void testContainersSortBeforeContained(RangeFactory<?> factory) {
        this.factory = factory;
        List<Range<?>> ranges = new ArrayList<>();
        ranges.add(of(1, 10));
        ranges.add(of(1, 9));
        ranges.add(of(1, 8));
        ranges.add(of(1, 7));
        ranges.add(of(1, 6));
        ranges.add(of(1, 5));
        ranges.add(of(1, 4));
        ranges.add(of(1, 3));
        ranges.add(of(1, 2));
        ranges.add(of(1, 1));
        Collections.sort(ranges);
        for (int i = 0; i < 10; i++) {
            Range<?> rng = ranges.get(i);
            assertEquals(10 - i, size(rng));
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("factories")
    @SuppressWarnings({"unchecked", "rawType"})
    public void testCoalesceEmpty(RangeFactory<?> factory) {
        this.factory = factory;
        assertEquals(0, Range.coalesce((List) Collections.emptyList(), (Coalescer) factory).size());
        List<Range<?>> l = new ArrayList();
        assertEquals(0, Range.coalesce((List) Collections.emptyList(), (Coalescer) factory).size());
        assertSame(l, Range.coalesce((List) l, (Coalescer) factory));

        List<Range<?>> r = arrayList(of(10, 10));
        assertEquals(r, Range.coalesce((List) r, (Coalescer) factory));
        assertSame(r, Range.coalesce((List) r, (Coalescer) factory));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("factories")
    public void testLongOnlyValues(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> x = factory.range(1, 1);
        if (x instanceof LongRange<?>) {
            long base = Integer.MAX_VALUE;
            Range<?> a = factory.range(base, 100L);
            Range<?> b = factory.range(base + 10, 10L);
            assertEquals(base, a.startValue().longValue());
            assertEquals(base + 10L, b.startValue().longValue());
            assertEquals(100L, a.sizeValue().longValue());
            assertTrue(a.contains(b));
            assertEquals(b, a.overlapWith(b));
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("factories")
    public void testAllEmptyRangesAreEqual(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> r = factory.range(0, 0);
        for (int i = 0; i < 5; i++) {
            Range<?> r1 = factory.range(i, 0);
            assertEquals(r, r1, r.getClass().getSimpleName());
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("factories")
    public void testOverlap(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> r = of(0, 10);
        for (int i = 0; i < 9; i++) {
            Range<?> test = of(i, 10);

            Range<?> checkIt = of(i + 1, 9);

            List<? extends Range<?>> cnon = test.nonOverlap(checkIt);
            assertEquals(1, cnon.size());
            assertEquals(1, cnon.get(0).sizeValue().intValue());
            assertEquals(i, cnon.get(0).startValue().intValue());

            cnon = test.nonOverlap(Range.of(checkIt.startValue().longValue(), checkIt.sizeValue().longValue()));
            assertEquals(1, cnon.size());
            assertEquals(1, cnon.get(0).sizeValue().intValue());
            assertEquals(i, cnon.get(0).startValue().intValue());

            cnon = test.nonOverlap(Range.of(checkIt.startValue().intValue(), checkIt.sizeValue().intValue()));
            assertEquals(1, cnon.size());
            assertEquals(1, cnon.get(0).sizeValue().intValue());
            assertEquals(i, cnon.get(0).startValue().intValue());

            Range<?> checkIt2 = of(i, 9);
            List<? extends Range<?>> cnon2 = test.nonOverlap(checkIt2);
            assertEquals(1, cnon2.size());
            assertEquals(1, cnon2.get(0).sizeValue().intValue());
            assertEquals(i + 9, cnon2.get(0).startValue().intValue());

            assertTrue(r.contains(i));
            assertTrue(r.overlaps(test));
            assertTrue(test.overlaps(r));
            Range<?> expectedOverlap = of(i, 10 - i);
            assertEquals(expectedOverlap, r.overlapWith(test), "Expected overlap " + expectedOverlap + " for " + test + " in " + r + " with " + factory.getClass().getSimpleName());
            assertEquals(expectedOverlap, test.overlapWith(r), "Expected rev-overlap " + expectedOverlap + " for " + test + " in " + r + " with " + factory.getClass().getSimpleName());
            List<? extends Range<?>> no = r.nonOverlap(test);
            List<? extends Range<?>> noRev = test.nonOverlap(r);
            switch (i) {
                case 0:
                    assertTrue(no.isEmpty());
                    break;
                default:
                    Range<?> startNon = of(0, i);
                    Range<?> endNon = of(10, i);
                    assertEquals(no, Arrays.asList(startNon, endNon), factory.getClass().getSimpleName());
                    assertEquals(noRev, Arrays.asList(startNon, endNon), factory.getClass().getSimpleName());
            }

        }
        assertFalse(r.contains(10));

        List<? extends Range<?>> no = of(0, 10).nonOverlap(of(100, 110));
        assertTrue(no.isEmpty(), "Should be no overlap of non-overlapping ranges");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("factories")
    public void testContains(RangeFactory<?> factory, String name) {
        this.factory = factory;
        assertTrue(of(0, 10).contains(of(1, 8)), factory.getClass().getSimpleName());
        assertFalse(of(1, 9).contains(of(0, 10)), factory.getClass().getSimpleName());
        assertTrue(of(0, 10).contains(ofCoordinates(1, 9)), factory.getClass().getSimpleName());
        assertTrue(of(0, 10).contains(ofCoordinates(1, 9)), factory.getClass().getSimpleName());
        Range<?> b = of(0, 10);
        for (int x = 0; x < 10; x++) {
            for (int y = x + 1; y < 10; y++) {
                Range<?> test = ofCoordinates(x, y);
                assertEquals(y - x, test.sizeValue().intValue(), "Test factory is broken for size: " + factory);
                assertEquals(x, test.startValue().intValue(), "Test factory is broken for start: " + factory);
                assertTrue(b.contains(test), b + " should contain " + test
                        + " with " + factory.getClass().getSimpleName());
            }
        }
    }

    @ParameterizedTest(name = "foo=''{0}''")
    @MethodSource("factories")
    @SuppressWarnings({"unchecked", "rawType"})
    public void testCoalesce(RangeFactory<?> factory) {
        this.factory = factory;
        if (!factory.isData()) {
            return;
        }
        factory.resetNames();
        Range<?> a = of(0, 10);
        Range<?> b = of(5, 15);
        List<Range<?>> result = factory.coalesce(a, b);
        assertEquals(3, result.size());

        List<OI> expected = Arrays.asList(new OI(0, 5, "0"), new OI(5, 5, "01"), new OI(10, 10, "1"));
        assertEquals(expected, result, factory.toString());
        assertNames(result, "0", "01", "1");
    }

    @ParameterizedTest(name = "foo=''{0}''")
    @MethodSource("factories")
    public void testIllegal(RangeFactory<?> factory) {
        this.factory = factory;
        assertThrows(IllegalArgumentException.class, () -> {
            factory.range(-1, 10);
        }, factory.toString());
        assertThrows(IllegalArgumentException.class, () -> {
            factory.range(-1L, 10L);
        }, factory.toString());
        assertThrows(IllegalArgumentException.class, () -> {
            factory.range(1L, -10L);
        }, factory.toString());
        assertThrows(IllegalArgumentException.class, () -> {
            factory.range(1, -10);
        }, factory.toString());
        assertThrows(IllegalArgumentException.class, () -> {
            factory.range(Long.MAX_VALUE - 10L, 20L);
        }, factory.toString());
        Range<?> r = factory.range(1, 1);
        if (!(r instanceof LongRange<?>) && !(r instanceof GenericRange)) {
            assertThrows(IllegalArgumentException.class, () -> {
                factory.range(Integer.MAX_VALUE - 10, 20);
            }, factory.toString());
        }
        assertThrows(IllegalArgumentException.class, () -> {
            r.withEnd(-1);
        }, factory.toString());
        assertThrows(IllegalArgumentException.class, () -> {
            r.withStart(-1);
        }, factory.toString());
        assertThrows(IllegalArgumentException.class, () -> {
            r.grownBy(-20);
        }, factory.toString());
        assertThrows(IllegalArgumentException.class, () -> {
            r.shrunkBy(20);
        }, factory.toString());
    }

    @ParameterizedTest(name = "foo=''{0}''")
    @MethodSource("factories")
    @SuppressWarnings("unchecked")
    public void testStartEndMutation(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> test = factory.range(10, 10);
        assertFalse(test.isEmpty());
        String s = factory.toString() + " / " + test.getClass().getName();
        if (test instanceof MutableIntRange<?>) {
            MutableIntRange<? extends MutableIntRange<?>> r = (MutableIntRange<? extends MutableIntRange<?>>) test;
            assertTrue(r.setEnd(25), s);
            assertEquals(10, r.size(), s);
            assertEquals(15, r.start(), s);
            assertTrue(r.setStart(1), s);
            assertEquals(10, r.size(), s);
            assertEquals(1, r.start(), s);
            assertEquals(11, r.end(), s);
            assertTrue(r.setStop(12), s);
            assertEquals(10, r.size(), s);
            assertEquals(13, r.end(), s);
            assertEquals(3, r.start());
        } else if (test instanceof MutableLongRange<?>) {
            MutableLongRange<? extends MutableLongRange<?>> r = (MutableLongRange<? extends MutableLongRange<?>>) test;
            assertTrue(r.setEnd(25), s);
            assertEquals(10, r.size(), s);
            assertEquals(15, r.start(), s);
            assertTrue(r.setStart(1), s);
            assertEquals(10, r.size(), s);
            assertEquals(1, r.start(), s);
            assertEquals(11, r.end(), s);
            assertTrue(r.setStop(12), s);
            assertEquals(10, r.size(), s);
            assertEquals(13, r.end(), s);
            assertEquals(3, r.start());
        }
    }

    @ParameterizedTest(name = "foo=''{0}''")
    @MethodSource("factories")
    public void testDistance(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> test = factory.range(10, 10);
        String s = factory.toString() + " / " + test.getClass().getName();
        Range<?> notAdjBefore = factory.range(0, 5);
        Range<?> notAdjAfter = factory.range(26, 5);
        assertEquals(5L, test.distance(notAdjBefore), s);
        assertEquals(5L, notAdjBefore.distance(test), s);
        assertEquals(6L, notAdjAfter.distance(test), s);
        assertEquals(6L, test.distance(notAdjAfter), s);
        Range<?> adjAfter = factory.range(20, 10);
        Range<?> adjBefore = factory.range(0, 10);
        assertEquals(0L, test.distance(adjAfter), s + " distance " + test + " and " + adjAfter);
        assertEquals(0L, test.distance(adjBefore), s + " distance " + test + " and " + adjBefore);
        assertEquals(0L, adjAfter.distance(test), s + " distance " + adjAfter + " and " + test);
        assertEquals(0L, adjBefore.distance(test), s + " distance " + adjBefore + " and " + test);
        Range<?> contained = factory.range(11, 2);
        Range<?> contains = factory.range(8, 14);
        assertEquals(-1L, test.distance(contained), s);
        assertEquals(-1L, test.distance(contains), s);
        assertEquals(-1L, contains.distance(test), s);
        assertEquals(-1L, contained.distance(test), s);
    }

    @ParameterizedTest(name = "foo=''{0}''")
    @MethodSource("factories")
    public void testAdjacent(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> r = factory.range(10, 10);
        Range<?> adjAfter = factory.range(20, 10);
        Range<?> adjBefore = factory.range(0, 10);
        Range<?> notAdjBefore = factory.range(0, 5);
        Range<?> notAdjAfter = factory.range(15, 5);
        Range<?> contained = factory.range(11, 2);
        Range<?> contains = factory.range(8, 14);
        Range<?> containedSameStart = factory.range(10, 5);
        Range<?> containsSameStart = factory.range(10, 20);
        Range<?> containedSameEnd = factory.range(15, 5);
        Range<?> containsSameEnd = factory.range(5, 15);
        Range<?> overlapEmpty = factory.range(11, 0);
        Range<?> notAdjEmpty = factory.range(10, 0);

        assertTrue(r.abuts(adjBefore));
        assertTrue(adjBefore.abuts(r));

        assertTrue(r.abuts(adjAfter));
        assertTrue(adjAfter.abuts(r));

        for (Range<?> test : new Range<?>[]{notAdjBefore, notAdjAfter, contained,
            contains, containedSameStart, containsSameStart, containedSameEnd,
            containsSameEnd, overlapEmpty, overlapEmpty, notAdjEmpty}) {
            Range<?> t = test;
            assertFalse(r.abuts(test), r + " and " + test + " do abut but " + r + " says it does: " + r.getClass().getName() + " and " + test.getClass().getName());
            assertFalse(test.abuts(r), r + " and " + test + " do abut but " + r + " says it does: " + r.getClass().getName() + " and " + test.getClass().getName());
            t = Range.of(t.startValue().longValue(), t.sizeValue().longValue());
            assertFalse(r.abuts(test), r + " and " + test + " do abut but " + r + " says it does: " + r.getClass().getName() + " and " + test.getClass().getName());
            assertFalse(test.abuts(r), r + " and " + test + " do abut but " + r + " says it does: " + r.getClass().getName() + " and " + test.getClass().getName());
            t = Range.of(t.startValue().intValue(), t.sizeValue().intValue());
            assertFalse(r.abuts(test), r + " and " + test + " do abut but " + r + " says it does: " + r.getClass().getName() + " and " + test.getClass().getName());
            assertFalse(test.abuts(r), r + " and " + test + " do abut but " + r + " says it does: " + r.getClass().getName() + " and " + test.getClass().getName());
        }
    }

    @ParameterizedTest(name = "foo=''{0}''")
    @MethodSource("factories")
    public void testMutable(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> r = factory.range(1, 1);
        if (r instanceof MutableRange<?>) {
            if (r instanceof MutableIntRange<?>) {
                MutableIntRange<?> mir = (MutableIntRange<?>) r;
                mir.setSize(2);
                assertEquals(2, mir.size());
                assertEquals(1, mir.start());
                mir.setStart(2);
                assertEquals(2, mir.start());
                assertEquals(2, mir.size());
                mir.shift(1);
                assertEquals(3, mir.start());
                assertEquals(2, mir.size());
                mir.grow(1);
                assertEquals(3, mir.start());
                assertEquals(3, mir.size());
                mir.shrink(1);
                assertEquals(3, mir.start());
                assertEquals(2, mir.size());
                mir.resizeIfExact(3, 2, 20);
                assertEquals(3, mir.start());
                assertEquals(20, mir.size());

                mir.setStartAndSize(10, 10);
                assertEquals(10, mir.start());
                assertEquals(10, mir.size());

                mir.setStartAndSizeValues(20L, 20L);
                assertEquals(20, mir.start());
                assertEquals(20, mir.size());

                mir.resizeIfExact(3, 2, 50);
                assertEquals(20, mir.size());
            } else if (r instanceof MutableLongRange<?>) {
                MutableLongRange<?> mir = (MutableLongRange<?>) r;
                mir.setSize(2);
                assertEquals(2, mir.size());
                assertEquals(1, mir.start());
                mir.setStart(2);
                assertEquals(2, mir.start());
                assertEquals(2, mir.size());
                mir.shift(1);
                assertEquals(3, mir.start());
                assertEquals(2, mir.size());
                mir.grow(1);
                assertEquals(3, mir.start());
                assertEquals(3, mir.size());
                mir.shrink(1);
                assertEquals(3, mir.start());
                assertEquals(2, mir.size());
                mir.resizeIfExact(3, 2, 20);
                assertEquals(3, mir.start());
                assertEquals(20, mir.size());

                mir.setStartAndSize(10, 10);
                assertEquals(10, mir.start());
                assertEquals(10, mir.size());

                mir.setStartAndSizeValues(20L, 20L);
                assertEquals(20, mir.start());
                assertEquals(20, mir.size());

                mir.resizeIfExact(3, 2, 50);
                assertEquals(20, mir.size());
            }
        }
    }

    @ParameterizedTest(name = "foo=''{0}''")
    @MethodSource("factories")
    public void testHeteroTypeEquals(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> r = factory.range(37, 20);
        factories().forEach((Arguments a) -> {
            RangeFactory<?> rf = (RangeFactory<?>) a.get()[0];
            Range eq = rf.range(37, 20);
            assertEquals(r, eq);
            Range neq = rf.range(37, 19);
            assertNotEquals(r, neq);
            neq = rf.range(38, 19);
            assertNotEquals(r, neq);
            neq = rf.range(1, 1);
            assertNotEquals(r, neq);
            neq = rf.range(37, 0);
            assertNotEquals(r, neq);
            assertEquals(r.hashCode(), eq.hashCode(),
                    "Hash codes do not match between "
                    + r.getClass().getSimpleName()
                    + " and " + eq.getClass().getSimpleName());
        });
        assertNotEquals(r, "x");
        assertNotEquals(r, null);
    }

    @ParameterizedTest(name = "foo=''{0}''")
    @MethodSource("factories")
    public void testPositionRelations(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> r = factory.range(37, 20);
        com.mastfrog.range.PositionRelation rel = r.relationToStart(0);
        assertEquals(com.mastfrog.range.PositionRelation.LESS,
                rel, r.getClass().getSimpleName() + " " + factory);
        rel = r.relationToStart(37);
        assertEquals(com.mastfrog.range.PositionRelation.EQUAL, rel, r.getClass().getSimpleName() + " " + factory);
        assertTrue(rel.isOverlap(), r.getClass().getSimpleName() + " " + factory);
        rel = r.relationToStart(38);
        assertEquals(com.mastfrog.range.PositionRelation.GREATER, rel, r.getClass().getSimpleName() + " " + factory);
        rel = r.relationToEnd(0);
        assertEquals(com.mastfrog.range.PositionRelation.LESS, rel, r.getClass().getSimpleName() + " " + factory);
        rel = r.relationToEnd(37);
        assertEquals(com.mastfrog.range.PositionRelation.LESS, rel, r.getClass().getSimpleName() + " " + factory);
        rel = r.relationToEnd(57);
        assertEquals(com.mastfrog.range.PositionRelation.EQUAL, rel, r.getClass().getSimpleName() + " " + factory);
        rel = r.relationToEnd(58);
        assertEquals(com.mastfrog.range.PositionRelation.GREATER, rel, r.getClass().getSimpleName() + " " + factory);
    }

    void assertNames(List<? extends Range<?>> ranges, String... names) {
        assertEquals(Arrays.asList(names), names(ranges), factory.toString());
    }

    @SuppressWarnings("unchecked")
    static List<String> names(List<? extends Range<?>> ranges) {
        List<String> result = new ArrayList<>(ranges.size());
        for (Range<?> r : ranges) {
            DataRange<String, ?> dr = (DataRange<String, ?>) r;
            result.add(dr.get());
        }
        return result;
    }

    @ParameterizedTest(name = "foo=''{0}'' {1}")
    @MethodSource("factories")
    @SuppressWarnings({"unchecked", "rawTypes"})
    public void testContainsPosition(RangeFactory<?> factory) {
        this.factory = factory;
        for (int i = 0; i < 20; i++) {
            for (int j = 1; j < 20; j++) {
                Range<?> a = of(i, j);
                for (int k = 0; k < i + j + 5; k++) {
                    if (k < i || k >= i + j) {
                        assertFalse(a.contains(k), factory + ": " + a + " should not contain " + k + " but claims to when passed an int");
                        assertFalse(a.contains((long) k), factory + ": " + a + " should not contain " + k + " but claims to when passed a long");
                    } else {
                        assertTrue(a.contains(k), factory + ": " + a + " should contain " + k + " but claims not to when passed an int");
                        assertTrue(a.contains((long) k), factory + ": " + a + " should contain " + k + " but claims not to when passed a long");
                    }
                }
            }
        }
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("factories")
    @SuppressWarnings({"unchecked", "rawTypes"})
    public void testContains(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> a = of(100, 100);
        Range<?> b = of(110, 10);
        assertTrue(a.contains(b));
        assertTrue(b.isContainedBy(a));
        b = of(10, 10);
        assertFalse(a.contains(b));
        assertFalse(b.isContainedBy(a));
        b = a;
        assertFalse(a.contains(b));
        assertFalse(b.isContainedBy(a));
        b = of(100, 100);
        assertFalse(a.contains(b));
        assertFalse(b.isContainedBy(a));

        assertEquals(a, a.overlapWith(a));
        assertSame(a, a.overlapWith(a));

        // Hetero types
        b = Range.of(110L, 10L);
        assertEquals(b, a.overlapWith(b));
        assertEquals(b, b.overlapWith(a));

        b = Range.of(110, 10);
        assertEquals(b, a.overlapWith(b));
        assertEquals(b, b.overlapWith(a));

        b = Range.of(10, 10);
        assertEquals(0, a.overlapWith(b).sizeValue().intValue());
        assertEquals(0, b.overlapWith(a).sizeValue().intValue());
        assertEquals(0L, Range.span(Collections.emptyList()).size());

        assertTrue(a.nonOverlap(a).isEmpty());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("factories")
    @SuppressWarnings({"unchecked", "rawTypes"})
    public void testSimpleOverlap(RangeFactory<?> factory) {
        this.factory = factory;
        Range<? extends Range<?>> a = of(0, 10);

        for (int i = 0; i < 20; i++) {
            if (i < 10) {
                assertTrue(a.contains(i), factory + ": " + a + " should contain "
                        + i + " but claims not to when passed an int - "
                        + a.getClass().getSimpleName());
                assertTrue(a.contains((long) i), factory + ": " + a + " should contain "
                        + i + " but claims not to when passed a long - "
                        + a.getClass().getSimpleName());
            } else {
                assertFalse(a.contains(i), factory + ": " + a + " should not contain "
                        + i + " but claims to when passed an int - "
                        + a.getClass().getSimpleName());
                assertFalse(a.contains((long) i), factory + ": " + a
                        + " should not contain " + i + " but claims to when passed a long - "
                        + a.getClass().getSimpleName());
            }
        }
        Range<? extends Range<?>> b = of(5, 15);
        for (int i = 0; i < 20; i++) {
            if (i >= 5 && i < 20) {
                assertTrue(b.contains(i), factory + ": " + b + " should not contain "
                        + i + " but claims to when passed an int - "
                        + b.getClass().getSimpleName());
                assertTrue(b.contains((long) i), factory + ": " + b + " should not contain "
                        + i + " but claims to when passed a long - " + b.getClass().getSimpleName());
            } else {
                assertFalse(b.contains(i), factory + ": " + b + " should contain "
                        + i + " but claims not to when passed an int - " + b.getClass().getSimpleName());
                assertFalse(b.contains((long) i), factory + ": " + b + " should contain "
                        + i + " but claims not to when passed a long - " + b.getClass().getSimpleName());
            }
        }
        RangeRelation rel1 = a.relationTo(b);
        assertEquals(STRADDLES_START, rel1);
        RangeRelation rel2 = b.relationTo(a);
        assertEquals(STRADDLES_END, rel2);
        assertTrue(a.overlaps(b));
        Range<?> overlap = a.overlapWith(b);
        assertNotNull(overlap);
        assertFalse(overlap.isEmpty());
        assertEquals(5, overlap.startValue().intValue());
        assertEquals(5, overlap.sizeValue().intValue());
        List<? extends Range<?>> non = a.nonOverlap(b);
        assertEquals(2, non.size());
        assertEquals(5, non.get(0).sizeValue().intValue());
        assertEquals(10, non.get(1).sizeValue().intValue());
        assertEquals(0, non.get(0).startValue().intValue());
        assertEquals(10, non.get(1).startValue().intValue());

        List<? extends Range<?>> coalesced;
        String s;

        coalesced = factory.coalesce(a, b);
        s = factory + ": " + coalesced.toString();
        assertEquals(3, coalesced.size(), s);
        assertEquals(0, coalesced.get(0).startValue().intValue(), s);
        assertEquals(5, coalesced.get(0).sizeValue().intValue(), s);
        assertEquals(5, coalesced.get(1).startValue().intValue(), s);
        assertEquals(5, coalesced.get(1).sizeValue().intValue(), s);
        assertEquals(10, coalesced.get(2).startValue().intValue(), s);
        assertEquals(10, coalesced.get(2).sizeValue().intValue(), s);
        Range<?> c = of(100, 10);
        coalesced = Range.coalesce((List) Arrays.asList(a, b, c), (Coalescer) factory);
        s = factory + ": " + coalesced.toString();
        assertEquals(4, coalesced.size(), s);
        assertEquals(0, coalesced.get(0).startValue().intValue(), s);
        assertEquals(5, coalesced.get(0).sizeValue().intValue(), s);
        assertEquals(5, coalesced.get(1).startValue().intValue(), s);
        assertEquals(5, coalesced.get(1).sizeValue().intValue(), s);
        assertEquals(10, coalesced.get(2).startValue().intValue(), s);
        assertEquals(10, coalesced.get(2).sizeValue().intValue(), s);
        assertEquals(100, coalesced.get(3).startValue().intValue(), s);
        assertEquals(10, coalesced.get(3).sizeValue().intValue(), s);
        assertSame(coalesced.get(3), c);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawType"})
    public void testCoalesceGeneric() {
        DataFixedLongRange a1 = (DataFixedLongRange) Range.of(5L, 43L, "23");
        DataFixedLongRange a2 = (DataFixedLongRange) Range.of(10L, 24L, "4");

        GenericRange g1 = new GenericRange(5L, 43L, "23");
        GenericRange g2 = new GenericRange(10L, 24L, "4");

        assertEquals(a1.overlapWith(a2), g1.overlapWith(g2), "getOverlap mismatch");
        assertEquals(a2.overlapWith(a1), g2.overlapWith(g1), "getOverlap mismatch");

        assertEquals(a1.overlaps(a2), g1.overlaps(g2), "Overlaps mismatch");
        assertEquals(a2.overlaps(a1), g2.overlaps(g1), "Overlaps mismatch");

        assertEquals(a1.nonOverlap(a2), g1.nonOverlap(g2), "Non-overlap mismatch");
        assertEquals(a2.nonOverlap(a1), g2.nonOverlap(g1), "Non-overlap mismatch");

        List<? extends LongRange<?>> expected = a1.coalesce(a2, new LongRangeFactory());
        List<? extends GenericRange> got = g1.coalesce(g2, new GenericRangeFactory());

        assertEquals(expected, got);
    }

    @ParameterizedTest(name = "foo=''{0}'' {1}")
    @MethodSource("factories")
    @SuppressWarnings({"unchecked", "rawType"})
    public void testCoalesceBuglet(RangeFactory<?> factory) {
        this.factory = factory;
        factory.resetNames();
        if (!factory.isData()) {
            return;
        }
        Range<?> r1 = of(0, 16);
        Range<?> r2 = of(1, 20);
        Range<?> r3 = of(2, 46);
        Range<?> r4 = of(5, 54);
        Range<?> r5 = of(10, 24);
        Range<?> r6 = of(11, 11);
        Range<?> r7 = of(12, 39);
        Range<?> r8 = of(13, 17);

//        List<Range<? extends Range<?>>> ranges = Arrays.asList(/*r1, r2, */ r3, r4, r5/*, r6, r7, r8*/);
        List<Range<? extends Range<?>>> ranges = Arrays.asList(r1, r2, r3, r4, r5, r6, r7, r8);

        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        for (Range r : ranges) {
            max = Math.max(max, r.startValue().longValue() + r.sizeValue().longValue());
            min = Math.min(min, r.startValue().longValue());
        }
        assertFalse(max == Long.MIN_VALUE);

        List<Range<? extends Range<?>>> coa = Range.coalesce((List) ranges, (Coalescer) factory);
        assertFalse(coa.isEmpty());

        LongRange<? extends LongRange<?>> span = Range.span(coa);

        assertNotNull(span);
        assertFalse(span.isEmpty());
        assertEquals(min, span.start());
        assertEquals(max, span.end());

        for (Range<? extends Range<?>> r : coa) {
            String nm = nameOf(r);
            r.forEachPosition((int pos) -> {
                Set<Character> ch = new TreeSet<>();
                Set<Range<?>> matches = new HashSet<>();
                for (Range<?> rx : ranges) {
                    if (rx.contains(pos)) {
                        matches.add(rx);
                        String name = nameOf(rx);
                        for (char c : name.toCharArray()) {
                            ch.add(c);
                        }
                    }
                }
                assertFalse(ch.isEmpty(), "Nothing found for " + r);
                StringBuilder sb = new StringBuilder();
                for (Character c : ch) {
                    sb.append(c);
                }
                List<Range<?>> rr = new ArrayList<>(matches);
                Collections.sort(rr);
                assertEquals(nm, sb.toString(), factory + " exp " + nm + " got " + sb
                        + " matching " + r + " + at " + pos + " with " + rr);
            });
        }

        long[] minMax = new long[]{Integer.MAX_VALUE, Integer.MIN_VALUE};
        span.forEachPosition((int pos) -> {
            minMax[0] = Math.min(pos, minMax[0]);
            minMax[1] = Math.max(pos, minMax[1]);
        });
        assertEquals(span.start(), minMax[0]);
        assertEquals(span.stop(), minMax[1]);
        minMax[0] = Long.MAX_VALUE;
        minMax[1] = Long.MIN_VALUE;
        span.forEachPosition((long pos) -> {
            minMax[0] = Math.min(pos, minMax[0]);
            minMax[1] = Math.max(pos, minMax[1]);
        });
        assertEquals(span.start(), minMax[0]);
        assertEquals(span.stop(), minMax[1]);
    }

    @ParameterizedTest(name = "foo=''{0}'' {1}")
    @MethodSource("factories")
    public void testEachPosition(RangeFactory<?> factory) {
        this.factory = factory;
        long[] minMax = new long[]{Integer.MAX_VALUE, Integer.MIN_VALUE};
        factory.range(100, 100).forEachPosition((int pos) -> {
            minMax[0] = Math.min(pos, minMax[0]);
            minMax[1] = Math.max(pos, minMax[1]);
        });
        assertEquals(100L, minMax[0]);
        assertEquals(199L, minMax[1]);
        minMax[0] = Long.MAX_VALUE;
        minMax[1] = Long.MIN_VALUE;
        factory.range(100, 100).forEachPosition((long pos) -> {
            minMax[0] = Math.min(pos, minMax[0]);
            minMax[1] = Math.max(pos, minMax[1]);
        });
        assertEquals(100L, minMax[0]);
        assertEquals(199L, minMax[1]);
    }

    @ParameterizedTest(name = "foo=''{0}'' {1}")
    @MethodSource("factories")
    @SuppressWarnings({"unchecked", "rawType"})
    public void testCoalesceRandom(RangeFactory<?> factory) {
        this.factory = factory;
        if (!factory.isData()) {
            return;
        }
        Random rnd = new Random(30192);
        for (int i = 0; i < 1000; i++) {
            doOneRandom(i, rnd);
        }
    }

    @SuppressWarnings({"unchecked", "rawType"})
    private void doOneRandom(int ix, Random rnd) {
        factory.resetNames();
        int size = ALPHA.length;
        Set<Integer> sets = new TreeSet<>();
        int count = rnd.nextInt(size - 10) + 10;
        for (int i = 0; i < count; i++) {
            sets.add(rnd.nextInt(count));
        }

        Iterator<Integer> iter = sets.iterator();
        int[] starts = new int[sets.size()];
        int[] sizes = new int[sets.size()];
        String[] names = new String[sets.size()];
        List<Range<?>> ois = new ArrayList<>();
        for (int i = 0; i < sets.size(); i++) {
            int start = iter.next();
            int sz = rnd.nextInt((size - i) + 1) + 1;

            ois.add(of(start, sz));
            names[i] = factory.lastName();
            starts[i] = start;
            sizes[i] = sz;
        }
        List<Range<?>> coa = Range.coalesce((List) ois, (Coalescer) factory);
        for (Range<?> oi : coa) {
            List<String> expectedNames = new ArrayList<>(20);
            for (int i = 0; i < starts.length; i++) {
                Range<?> r = Range.of(starts[i], sizes[i]);
                if (r.overlaps(oi)) {
                    expectedNames.add(names[i]);
                }
            }
            Collections.sort(expectedNames);
            StringBuilder sb = new StringBuilder();
            for (String n : expectedNames) {
                sb.append(n);
            }
            String nm = ((DataRange<String, ?>) oi).get();
            assertNotNull(nm, "No name for " + oi + " " + oi.getClass().getName());
            if (!nm.equals(sb.toString())) {
                fail(ix + ":" + factory + ": Name mismatch " + oi + " should be " + sb + " for " + ois);
            }
        }
    }

    @Test
    public void testHeterogenous() {
        Range<?> a = Range.of(0L, 10L);
        Range<?> b = Range.of(5, 10);
        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
        assertFalse(a.contains(b));
        assertFalse(b.contains(a));
        assertFalse(a.isContainedBy(b));
        assertFalse(b.isContainedBy(a));
        b = Range.of(100, 110);
        assertFalse(a.overlaps(b));
        assertFalse(b.overlaps(a));
        b = new GenericRange(100, 110);
        assertFalse(a.overlaps(b));
        assertFalse(b.overlaps(a));
        b = new GenericRange(5L, 10L);
        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
        b = new GenericRange(5, 10);
        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    public void testAbuts(RangeFactory<?> factory) {
        this.factory = factory;

        Range<?> r = of(100, 100);
        assertTrue(r.abuts(Range.of(0, 100)));
        assertTrue(r.abuts(Range.of(0L, 100L)));

        assertFalse(r.abuts(Range.of(0, 99)));
        assertFalse(r.abuts(Range.of(0L, 99L)));

        assertTrue(r.abuts(Range.of(200, 100)));
        assertTrue(r.abuts(Range.of(200L, 100L)));

        assertFalse(r.abuts(Range.of(201, 100)));
        assertFalse(r.abuts(Range.of(201L, 100L)));

        Range<?> a = factory.ofCoordinates(8, 10);
        Range<?> b = factory.ofCoordinates(10, 18);
        String nm = a.getClass().getSimpleName();

        assertTrue(a.abuts(b), nm);
        assertTrue(b.abuts(a), nm);
        a = factory.ofCoordinates(10, 19);
        b = factory.ofCoordinates(19, 22);
        assertTrue(a.abuts(b), nm);
        assertTrue(b.abuts(a), nm);
        b = factory.ofCoordinates(20, 22);
        assertFalse(a.abuts(b), nm);
        assertFalse(b.abuts(a), nm);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    public void testExtremeRelations(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> r = of(100, 100);

        RangePositionRelation rel = r.relationTo(Long.MAX_VALUE);
        assertEquals(RangePositionRelation.AFTER, rel);
        rel = r.relationTo(Long.MIN_VALUE);
        assertEquals(RangePositionRelation.BEFORE, rel);

        rel = r.relationTo(Integer.MAX_VALUE);
        assertEquals(RangePositionRelation.AFTER, rel);
        rel = r.relationTo(Integer.MIN_VALUE);
        assertEquals(RangePositionRelation.BEFORE, rel);
    }

    @Test
    public void testContainsBoundary() {
        IntRange<? extends IntRange<?>> a = Range.of(10, 10);
        LongRange<? extends LongRange<?>> b = Range.of(15L, 10L);
        LongRange<? extends LongRange<?>> c = Range.of(150L, 10L);
        assertTrue(a.containsBoundary(b));
        assertTrue(b.containsBoundary(a));

        assertFalse(a.containsBoundary(c));
        assertFalse(b.containsBoundary(c));
        assertFalse(c.containsBoundary(a));
        assertFalse(c.containsBoundary(b));

        assertFalse(a.containsBoundary(a));
        assertFalse(b.containsBoundary(b));
        GenericRange ga = new GenericRange(10, 10);
        GenericRange gb = new GenericRange(15, 10);
        GenericRange gc = new GenericRange(150, 10);

        assertTrue(ga.containsBoundary(b));
        assertTrue(gb.containsBoundary(a));
        assertTrue(ga.containsBoundary(gb));
        assertTrue(gb.containsBoundary(ga));

        assertFalse(a.containsBoundary(c));
        assertFalse(b.containsBoundary(c));
        assertFalse(c.containsBoundary(a));
        assertFalse(c.containsBoundary(b));

        assertFalse(ga.containsBoundary(c));
        assertFalse(gb.containsBoundary(c));
        assertFalse(gc.containsBoundary(a));
        assertFalse(gc.containsBoundary(b));

        assertFalse(ga.containsBoundary(gc));
        assertFalse(gb.containsBoundary(gc));
        assertFalse(gc.containsBoundary(ga));
        assertFalse(gc.containsBoundary(gb));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    public void testSnapshot(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> r = of(10, 10);
        Range<?> snapshot = null;
        if (r instanceof IntRange<?>) {
            snapshot = ((IntRange<?>) r).snapshot();
            assertNotSame(r, snapshot);
        } else if (r instanceof LongRange<?>) {
            snapshot = ((LongRange<?>) r).snapshot();
            assertNotSame(r, snapshot);
        }
        if (snapshot != null) {
            assertEquals(r, snapshot);
            assertFalse(snapshot instanceof MutableRange<?>);
            if (r instanceof MutableIntRange<?>) {
                ((MutableIntRange<?>) r).setSize(9);
                assertEquals(9, r.sizeValue().intValue());
                assertNotEquals(r, snapshot, r + " should not equal " + snapshot + " " + factory + " " + r.getClass().getSimpleName());
            } else if (r instanceof MutableLongRange<?>) {
                ((MutableLongRange<?>) r).setSize(9);
                assertEquals(9, r.sizeValue().intValue());
                assertNotEquals(r, snapshot, r + " should not equal " + snapshot + " " + factory + " " + r.getClass().getSimpleName());
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    public void testCoalesceMany(RangeFactory<?> factory) {
        this.factory = factory;
        if (!factory.isData()) {
            return;
        }
        factory.resetNames();
        OI a = new OI(0, 10, "A");
        OI b = new OI(5, 15, "B");
        OI c = new OI(6, 3, "C");
        OI d = new OI(6, 2, "D");
        OI e = new OI(7, 1, "E");
        OI f = new OI(8, 3, "F");
        OI g = new OI(18, 3, "G");

        List<OI> all = arrayList(a, b, c, d, e, f, g);

        List<OI> got = Range.coalesce(all, new C());

        OI last = null;
        for (OI oi : got) {
            if (last != null) {
                assertTrue(last.abuts(oi));
                assertTrue(oi.abuts(last));
                assertFalse(last.overlaps(oi));
                assertFalse(oi.overlaps(last));
            }
            last = oi;
            switch (oi.start) {
                case 0:
                    assertEquals("A", oi.name);
                    break;
                case 5:
                    assertEquals("AB", oi.name);
                    break;
                case 6:
                    assertEquals("ABCD", oi.name);
                    break;
                case 7:
                    assertEquals("ABCDE", oi.name);
                    break;
                case 8:
                    assertEquals("ABCF", oi.name);
                    break;
                case 9:
                    assertEquals("ABF", oi.name);
                    break;
                case 10:
                    assertEquals("BF", oi.name);
                    break;
                case 11:
                    assertEquals("B", oi.name);
                    break;
                case 18:
                    assertEquals("BG", oi.name);
                    break;
                case 20:
                    assertEquals("G", oi.name);
                    break;
            }
        }
    }

    static String nameOf(Range<?> range) {
        assertTrue(range instanceof DataRange<?, ?>, range.getClass().getName());
        return ((DataRange<?, ?>) range).get() + "";
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("factories")
    public void testGrowShrink(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> rng = factory.range(10, 20);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        rng = rng.grownBy(10);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(30, rng.sizeValue().intValue());
        rng = rng.shrunkBy(10);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        rng = rng.grownBy((long) 10);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(30, rng.sizeValue().intValue());
        rng = rng.shrunkBy((long) 10);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        rng = rng.shiftedBy(10);
        assertEquals(20, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        rng = rng.shiftedBy(-10);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        rng = rng.shiftedBy((long) 10);
        assertEquals(20, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        rng = rng.shiftedBy((long) -10);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        Range<?> old = rng;
        rng = rng.shiftedBy(0L);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        assertSame(old, rng);
        rng = rng.shiftedBy(0);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        assertSame(old, rng);
        rng = rng.grownBy(0);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        assertSame(old, rng);
        rng = rng.grownBy(0L);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        assertSame(old, rng);
        rng = rng.shrunkBy(0);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        assertSame(old, rng);
        rng = rng.shrunkBy(0L);
        assertEquals(10, rng.startValue().intValue());
        assertEquals(20, rng.sizeValue().intValue());
        assertSame(old, rng);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("factories")
    public void testCoalesceNonOverlapping(RangeFactory<?> factory) {
        this.factory = factory;
        factory.resetNames();
        Range<?> a = factory.range(0, 10);
        Range<?> b = factory.range(20, 10);
        List<Range<?>> r = factory.coalesce(a, b);
        assertEquals(2, r.size());
        assertSame(a, r.get(0));
        assertSame(b, r.get(1));
        r = factory.coalesce(b, a);
        assertEquals(2, r.size());
        assertSame(a, r.get(0));
        assertSame(b, r.get(1));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("factories")
    public void testCoalesceEmpty2(RangeFactory<?> factory) {
        this.factory = factory;
        factory.resetNames();
        Range a = factory.range(0, 0);
        Range b = factory.range(10, 0);
        List<Range<?>> r = factory.coalesce(a, b);
        assertTrue(r.isEmpty());

        a = factory.range(0, 10);
        r = factory.coalesce(a, b);
        assertEquals(1, r.size());
        assertSame(a, r.get(0));
        r = factory.coalesce(b, a);
        assertEquals(1, r.size());
        assertSame(a, r.get(0));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    public void testSubtract(RangeFactory<?> factory) {
        this.factory = factory;
        Range<?> r = factory.range(100, 100);

        // Non overlapping ranges should return the original range
        // being subtracted from
        assertFalse(r.subtracting(factory.range(10, 10)).isEmpty());
        assertTrue(r.subtracting(factory.range(10, 10)).contains(r));
        assertFalse(r.subtracting(factory.range(300, 10)).isEmpty());
        assertTrue(r.subtracting(factory.range(300, 10)).contains(r));
        assertFalse(r.subtracting(factory.range(200, 10)).isEmpty());
        assertTrue(r.subtracting(factory.range(200, 10)).contains(r));

        List<? extends Range<?>> l = r.subtracting(factory.range(110, 80));

        assertEquals(2, l.size(), l + "");
        Range<?> a = l.get(0);
        Range<?> b = l.get(1);
        assertEquals(r.startValue().intValue(), a.startValue().intValue());
        assertEquals(190, b.startValue().intValue());
        assertEquals(10, a.sizeValue().intValue());
        assertEquals(10, b.sizeValue().intValue());

        l = r.subtracting(factory.range(110, 0));
        assertTrue(l.contains(r));
        assertEquals(1, l.size());

        l = r.subtracting(factory.range(150, 50));
        assertEquals(1, l.size());
        a = l.get(0);
        assertEquals(50, a.sizeValue().intValue());
        assertEquals(100, a.startValue().intValue());

        l = r.subtracting(factory.range(150, 100));
        assertEquals(1, l.size());
        a = l.get(0);
        assertEquals(50, a.sizeValue().intValue());
        assertEquals(100, a.startValue().intValue());

        l = r.subtracting(factory.range(100, 99));
        assertEquals(1, l.size());
        a = l.get(0);
        assertEquals(199, a.startValue().intValue());
        assertEquals(1, a.sizeValue().intValue());

        l = r.subtracting(factory.range(100, 40));
        assertEquals(1, l.size());
        a = l.get(0);
        assertEquals(140, a.startValue().intValue());
        assertEquals(60, a.sizeValue().intValue());

        assertTrue(r.subtracting(r).isEmpty());
        assertTrue(factory.range(110, 80).subtracting(r).isEmpty());
    }

    @SafeVarargs
    private static <T> List<T> arrayList(T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    static final class C implements Coalescer<OI> {

        @Override
        public OI combine(OI a, OI b, int start, int size) {
            String[] nms = new String[]{a.name, b.name};
            Arrays.sort(nms);
            return new OI(start, size, nms[0] + nms[1]);
        }

        @Override
        public OI resized(OI orig, int start, int size) {
            return new OI(start, size, orig.name);
        }
    }

    static final class OI implements DataIntRange<String, OI> {

        private final int start;
        private final int size;
        private final String name;

        public OI(int start, int size, String name) {
            RangeHolder.checkStartAndSize(start, size);
            this.start = start;
            this.size = size;
            this.name = name;
        }

        @Override
        public String get() {
            return name;
        }

        @Override
        public String toString() {
            return "'" + name + "' " + start() + ":" + end() + " (" + size() + ")";
        }

        @Override
        public int start() {
            return start;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public int hashCode() {
            long hash = 7;
            hash = 37 * hash + this.start;
            hash = 37 * hash + this.size;
            return (int) (hash ^ (hash << 32));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Range<?>) {
                if (((Range) obj).isEmpty() && isEmpty()) {
                    return true;
                }
                return matches((Range<?>) obj);
            }
            return false;
        }

        @Override
        public OI newRange(int start, int size) {
            return new OI(start, size, name);
        }

        @Override
        public OI newRange(long start, long size) {
            return new OI((int) start, (int) size, name);
        }

        @Override
        public DataRange<String, OI> newRange(int start, int size, String newObject) {
            return new OI(start, size, newObject);
        }

        @Override
        public DataRange<String, OI> newRange(long start, long size, String newObject) {
            return new OI((int) start, (int) size, newObject);
        }
    }

    public void assertRelation(RangeRelation relation, Range<?> a, Range<?> b) {
        StringBuilder sb = new StringBuilder();
        char[] spaces = new char[Math.max(a.startValue().intValue() + a.sizeValue().intValue(), b.startValue().intValue() + b.sizeValue().intValue())
                - Math.min(a.startValue().intValue(), b.startValue().intValue()) + 25];
        sb.append(spaces);
        Arrays.fill(spaces, ' ');
        sb.setCharAt(a.startValue().intValue(), '[');
        sb.setCharAt(b.startValue().intValue(), '<');
        sb.setCharAt(a.startValue().intValue() + a.sizeValue().intValue(), ']');
        sb.setCharAt(b.startValue().intValue() + b.sizeValue().intValue(), '>');
        RangeRelation got = a.relationTo(b);
        sb.append(got.name());
        sb.append(a).append(' ').append(b);
        assertEquals(relation, got, sb.toString());
    }

    public static Stream<Arguments> factories() {
        return Stream.of(
                Arguments.of(new IntRangeFactory(), "IntRange"),
                Arguments.of(new LongRangeFactory(), "LongRange"),
                Arguments.of(new MutableIntRangeFactory(), "MutableIntRange"),
                Arguments.of(new MutableLongRangeFactory(), "MutableIntRange"),
                Arguments.of(new SyncMutableIntRangeFactory(), "SyncMutableIntRange"),
                Arguments.of(new SyncMutableLongRangeFactory(), "SyncMutableLongRange"),
                Arguments.of(new DataIntRangeFactory(), "DataIntRange"),
                Arguments.of(new DataLongRangeFactory(), "DataLongRange"),
                Arguments.of(new OIIntRangeFactory(), "OIRange"),
                Arguments.of(new GenericRangeFactory(), "GenericRange") // ensures fallback logic is tested
        );
    }

    static abstract class RangeFactory<R extends Range<? extends R>> implements Coalescer<R> {

        public abstract R range(int start, int size);

        public abstract R range(long start, long size);

        public abstract R ofCoordinates(int start, int end);

        public abstract R ofCoordinates(long start, long end);

        @SuppressWarnings("unchecked")
        protected R cast(Range<?> rng) {
            return (R) rng;
        }

        public abstract int startOf(Range<?> range);

        public abstract int sizeOf(Range<?> range);

        public boolean isData() {
            return false;
        }

        @Override
        public R combine(R a, R b, int start, int size) {
//            throw new UnsupportedOperationException("Not supported by " + getClass().getSimpleName());
            R creator = a.contains(start) ? a : b;
            return creator.newRange(start, size);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }

        @SuppressWarnings("unchecked")
        public List<Range<?>> coalesce(Range<?> a, Range<?> b) {
            Range ra = cast(a);
            Range rb = cast(b);
            List<Range<?>> result = ra.coalesce(rb, (Coalescer) this);
            return result;
        }

        RangeFactory<?> resetNames() {
            // do nothing
            return this;
        }

        private int count = 0;

        final String nextName() {
            if (count == ALPHA.length) {
                count = 0;
            }
            return "" + ALPHA[count++];
        }

        final String pendingName() {
            int ct = count + 1;
            if (ct == ALPHA.length) {
                ct = 0;
            }
            return "" + ALPHA[ct];
        }

        final String lastName() {
            int ct = count - 1;
            if (ct < 0) {
                ct = ALPHA.length - 1;
            }
            return "" + ALPHA[ct];
        }
    }

    static abstract class AbstractIntRangeFactory<R extends IntRange<? extends R>> extends RangeFactory<R> {

        @Override
        public final R range(long start, long end) {
            return range((int) start, (int) end);
        }

        @Override
        public final R ofCoordinates(long start, long end) {
            return ofCoordinates((int) start, (int) end);
        }

        @Override
        public int startOf(Range<?> range) {
            return cast(range).start();
        }

        @Override
        public int sizeOf(Range<?> range) {
            return cast(range).size();
        }
    }

    static abstract class AbstractLongRangeFactory<R extends LongRange<? extends R>> extends RangeFactory<R> {

        @Override
        public int startOf(Range<?> range) {
            return (int) cast(range).start();
        }

        @Override
        public int sizeOf(Range<?> range) {
            return (int) cast(range).size();
        }

        @Override
        public final R range(int start, int end) {
            return range((long) start, (long) end);
        }

        @Override
        public final R ofCoordinates(int start, int end) {
            return ofCoordinates((long) start, (long) end);
        }
    }

    static final class IntRangeFactory extends AbstractIntRangeFactory<IntRange<? extends IntRange<?>>> {

        @Override
        public IntRange<? extends IntRange<?>> range(int start, int size) {
            return Range.of(start, size);
        }

        @Override
        public IntRange<? extends IntRange<?>> ofCoordinates(int start, int end) {
            return Range.ofCoordinates(start, end);
        }

        public String toString() {
            return "IntRange";
        }

    }

    static final class LongRangeFactory extends AbstractLongRangeFactory<LongRange<? extends LongRange<?>>> {

        @Override
        public LongRange<? extends LongRange<?>> range(long start, long size) {
            return Range.of(start, size);
        }

        @Override
        public LongRange<? extends LongRange<?>> ofCoordinates(long start, long end) {
            return Range.ofCoordinates(start, end);
        }
    }

    static final class MutableIntRangeFactory extends AbstractIntRangeFactory<MutableIntRange<? extends MutableIntRange<?>>> {

        @Override
        public MutableIntRange<? extends MutableIntRange<?>> range(int start, int size) {
            return Range.mutableOf(start, size);
        }

        @Override
        public MutableIntRange<? extends MutableIntRange<?>> ofCoordinates(int start, int end) {
            return Range.mutableOfCoordinates(start, end);
        }
    }

    static final class MutableLongRangeFactory extends AbstractLongRangeFactory<MutableLongRange<? extends MutableLongRange<?>>> {

        @Override
        public MutableLongRange<? extends MutableLongRange<?>> range(long start, long size) {
            return Range.mutableOf(start, size);
        }

        @Override
        public MutableLongRange<? extends MutableLongRange<?>> ofCoordinates(long start, long end) {
            return Range.mutableOfCoordinates(start, end);
        }
    }

    static final class SyncMutableIntRangeFactory extends AbstractIntRangeFactory<MutableIntRange<? extends MutableIntRange<?>>> {

        @Override
        public MutableIntRange<? extends MutableIntRange<?>> range(int start, int size) {
            return Range.mutableOf(start, size, true);
        }

        @Override
        public MutableIntRange<? extends MutableIntRange<?>> ofCoordinates(int start, int end) {
            return Range.mutableOfCoordinates(start, end, true);
        }

    }

    static final class SyncMutableLongRangeFactory extends AbstractLongRangeFactory<MutableLongRange<? extends MutableLongRange<?>>> {

        @Override
        public MutableLongRange<? extends MutableLongRange<?>> range(long start, long size) {
            return Range.mutableOf(start, size, true);
        }

        @Override
        public MutableLongRange<? extends MutableLongRange<?>> ofCoordinates(long start, long end) {
            return Range.mutableOfCoordinates(start, end, true);
        }
    }

    static final class DataIntRangeFactory extends AbstractIntRangeFactory<DataIntRange<String, ? extends DataIntRange<String, ?>>> {

        @Override
        public DataIntRange<String, ? extends DataIntRange<String, ?>> range(int start, int size) {
            return Range.of(start, size, nextName());
        }

        @Override
        public DataIntRange<String, ? extends DataIntRange<String, ?>> ofCoordinates(int start, int end) {
            return Range.ofCoordinates(start, end, nextName());
        }

        @Override
        public boolean isData() {
            return true;
        }

        @Override
        public DataIntRange<String, ? extends DataIntRange<String, ?>> combine(DataIntRange<String, ? extends DataIntRange<String, ?>> a, DataIntRange<String, ? extends DataIntRange<String, ?>> b, int start, int size) {
            return Range.of(start, size, combineNames(a, b));
        }
    }

    static String combineNames(DataRange<String, ?> a, DataRange<String, ?> b) {
        StringBuilder sb = new StringBuilder();
        Set<Character> chars = new TreeSet<>();
        for (char c : a.get().toCharArray()) {
            chars.add(c);
        }
        for (char c : b.get().toCharArray()) {
            chars.add(c);
        }
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    static final class DataLongRangeFactory extends AbstractLongRangeFactory<DataLongRange<String, ? extends DataLongRange<String, ?>>> {

        @Override
        public DataLongRange<String, ? extends DataLongRange<String, ?>> range(long start, long size) {
            return Range.of(start, size, nextName());
        }

        @Override
        public DataLongRange<String, ? extends DataLongRange<String, ?>> ofCoordinates(long start, long end) {
            return Range.ofCoordinates(start, end, nextName());
        }

        @Override
        public boolean isData() {
            return true;
        }

        @Override
        public DataLongRange<String, ? extends DataLongRange<String, ?>> combine(DataLongRange<String, ? extends DataLongRange<String, ?>> a, DataLongRange<String, ? extends DataLongRange<String, ?>> b, int start, int size) {
            return Range.of((long) start, (long) size, combineNames(a, b));
        }
    }

    static final class OIIntRangeFactory extends AbstractIntRangeFactory<DataIntRange<String, ? extends DataIntRange<String, ?>>> {

        @Override
        public DataIntRange<String, ? extends DataIntRange<String, ?>> range(int start, int size) {
            return new OI(start, size, nextName());
        }

        @Override
        public DataIntRange<String, ? extends DataIntRange<String, ?>> ofCoordinates(int start, int end) {
            int st = Math.min(start, end);
            return new OI(st, Math.max(end, start) - st, nextName());
        }

        @Override
        public boolean isData() {
            return true;
        }

        @Override
        public DataIntRange<String, ? extends DataIntRange<String, ?>> combine(DataIntRange<String, ? extends DataIntRange<String, ?>> a, DataIntRange<String, ? extends DataIntRange<String, ?>> b, int start, int size) {
            return new OI(start, size, combineNames(a, b));
        }
    }

    static final class GenericRangeFactory extends RangeFactory<GenericRange> {

        @Override
        public GenericRange range(int start, int size) {
            return new GenericRange(start, size, nextName());
        }

        @Override
        public GenericRange range(long start, long size) {
            return new GenericRange(start, size, nextName());
        }

        @Override
        public GenericRange ofCoordinates(int start, int end) {
            int st = Math.min(start, end);
            return new GenericRange(st, Math.max(end, start) - st, nextName());
        }

        @Override
        public GenericRange ofCoordinates(long start, long end) {
            long st = Math.min(start, end);
            return new GenericRange(st, Math.max(end, start) - st, nextName());
        }

        @Override
        public int startOf(Range<?> range) {
            return range.startValue().intValue();
        }

        @Override
        public int sizeOf(Range<?> range) {
            return range.sizeValue().intValue();
        }

        @Override
        public boolean isData() {
            return true;
        }

        @Override
        public GenericRange combine(GenericRange a, GenericRange b, int start, int size) {
            String nm = combineNames(a, b);
            return new GenericRange(start, size, nm);
        }
    }

    private static final char[] ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    static {
        Arrays.sort(ALPHA);
    }

    static final class GenericRange implements MutableRange<GenericRange>, DataRange<String, GenericRange> {

        private String name;
        private Number start;
        private Number size;

        GenericRange(Number start, Number size) {
            this(start, size, null);
        }

        GenericRange(Number start, Number size, String name) {
            assertNotNull(start);
            assertNotNull(size);
            checkStartAndSize(start.longValue(), size.longValue());
            this.name = name;
            this.start = start;
            this.size = size;
        }

//        @Override
        public boolean setStartAndSize(int start, int size) {
            boolean result = this.start.intValue() != start || this.size.intValue() != size;
            this.start = start;
            this.size = size;
            return result;
        }

//        @Override
        public int start() {
            return start.intValue();
        }

//        @Override
        public int size() {
            return size.intValue();
        }

        @Override
        public GenericRange newRange(int start, int size) {
            return new GenericRange(start, size, name);
        }

        @Override
        public GenericRange newRange(long start, long size) {
            return new GenericRange(start, size, name);
        }

        @Override
        public DataRange<String, GenericRange> newRange(int start, int size, String newObject) {
            return new GenericRange(start, size, newObject);
        }

        @Override
        public DataRange<String, GenericRange> newRange(long start, long size, String newObject) {
            return new GenericRange(start, size, newObject);
        }

        @Override
        public String get() {
            return name;
        }

        @Override
        public String toString() {
            return "'" + name + "' " + start + ":" + (start.intValue() + size.intValue()) + " (" + size + ")";
        }

        public boolean equals(Object o) {
            return o instanceof Range<?> ? matches((Range<?>) o)
                    || (((Range<?>) o).isEmpty() && isEmpty()) : false;
        }

        @Override
        public int hashCode() {
            long hash = 7;
            hash = 37 * hash + this.start.longValue();
            hash = 37 * hash + this.size.longValue();
            return (int) (hash ^ (hash << 32));
        }

        @Override
        public boolean setStartAndSizeValues(Number start, Number size) {
            boolean result = !start.equals(this.start) || !size.equals(this.size);
            this.start = start;
            this.size = size;
            return result;
        }

        @Override
        public Number startValue() {
            return start;
        }

        @Override
        public Number sizeValue() {
            return size;
        }
    }
}
