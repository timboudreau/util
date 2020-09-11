package com.mastfrog.graph;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class PairSetTest {

    @Test
    public void testPairsStoredCorrectly() {
        PairSet set = new PairSet(20);
        Random rnd = new Random(20390237);
        Set<Coord> coords = new HashSet<>();
        for (int i = 0; i < 40; i++) {
            int x = rnd.nextInt(set.size());
            int y = rnd.nextInt(set.size());
            if (x != y) {
                Coord coord = new Coord(x, y);
                if (!coords.contains(coord)) {
                    coords.add(coord);
                    set.add(x, y);
                }
            }
        }
        for (int[] ints : set) {
            Coord coord = new Coord(ints);
            assertTrue(coords.contains(coord), "Iterator returned " + coord + " which should not be present");
        }
        PairSet inverted = set.inverse();
        assertEquals(set.size(), inverted.size());
        assertNotEquals(inverted, set);
        for (int[] ints : inverted) {
            Coord coord = new Coord(ints);
            assertFalse(coords.contains(coord));
        }
        for (int x = 0; x < set.size(); x++) {
            for (int y = 0; y < set.size(); y++) {
                Coord coord = new Coord(x, y);
                if (!coords.contains(coord)) {
                    assertFalse(set.contains(x, y), "Unexpected " + coord);
                    assertTrue(inverted.contains(x, y), "Inverted set should contain " + coord);
                } else {
                    assertTrue(set.contains(coord.x, coord.y), "Missing " + coord);
                    assertFalse(inverted.contains(x, y), "Inverted set should not contain " + coord);
                }
            }
        }
    }

    static final class Coord {

        public final int x, y;

        public Coord(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Coord(int[] xy) {
            assert xy.length == 2;
            this.x = xy[0];
            this.y = xy[1];
        }

        @Override
        public String toString() {
            return x + "," + y;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 13 * hash + this.x;
            hash = 13 * hash + this.y;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Coord other = (Coord) obj;
            if (this.x != other.x) {
                return false;
            }
            if (this.y != other.y) {
                return false;
            }
            return true;
        }
    }
}
