package com.mastfrog.range;

import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class RangeHolderTest {

    @Test
    @SuppressWarnings({"unchecked", "rawType"})
    public void testSomeMethod() {
        List<IntRange<? extends IntRange<?>>> l = Arrays.asList(Range.of(0, 100), Range.of(100, 110));

        RangeHolder rh = new RangeHolder(Range.of(0, 100));
        rh = rh.coalesce(Range.of(100, 110), null);
        assertEquals(l, rh.toList());

        assertTrue(rh.toString().contains(l.get(0).toString()));
        assertTrue(rh.toString().contains(l.get(1).toString()));
    }
}
