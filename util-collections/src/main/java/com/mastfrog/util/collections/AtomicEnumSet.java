/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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
package com.mastfrog.util.collections;

import java.util.Set;

/**
 * 32-and 64-bit thread-safe EnumSets using atomics.
 *
 * @author Tim Boudreau
 */
public interface AtomicEnumSet<E extends Enum<E>> extends Set<E> {

    /**
     * Create a copy of this set.
     *
     * @return A copy
     */
    AtomicEnumSet<E> copy();

    /**
     * Get the complement of this set - a set containing all items which are not
     * members of this set.
     *
     * @return A separate, complementing set.
     */
    AtomicEnumSet<E> complement();

    /**
     * Exposes the bitmask that is the data of this set; will be a long for
     * enums with more than 32 constants, otherwise will be 64 bits.
     *
     * @return
     */
    Number rawValue();

    public static <E extends Enum<E>> AtomicEnumSet<E> noneOf(Class<E> type) {
        E[] consts = type.getEnumConstants();
        if (consts.length <= 32) {
            return new AtomicEnumSetSmall<>(type);
        }
        if (consts.length > 64) {
            throw new IllegalArgumentException("Cannot handle enums > 64 constants");
        }
        return new AtomicEnumSetMedium<>(type);
    }

    public static <E extends Enum<E>> AtomicEnumSet<E> of(Class<E> type, Number value) {
        E[] consts = type.getEnumConstants();
        if (consts.length <= 32) {
            return new AtomicEnumSetSmall<>(type, value.intValue());
        }
        if (consts.length > 64) {
            throw new IllegalArgumentException("Cannot handle enums > 64 constants");
        }
        return new AtomicEnumSetMedium<>(type, value.longValue());
    }

    public static <E extends Enum<E>> AtomicEnumSet<E> of(E first, E second) {
        Class<E> type = first.getDeclaringClass();
        E[] consts = type.getEnumConstants();
        if (consts.length < 32) {
            return new AtomicEnumSetSmall<>(first, second);
        }
        if (consts.length > 64) {
            throw new IllegalArgumentException("Cannot handle enums > 64 constants");
        }
        return new AtomicEnumSetMedium<>(first, second);
    }

    public static <E extends Enum<E>> AtomicEnumSet<E> of(E first) {
        Class<E> type = first.getDeclaringClass();
        E[] consts = type.getEnumConstants();
        if (consts.length < 32) {
            return new AtomicEnumSetSmall<>(first);
        }
        if (consts.length > 64) {
            throw new IllegalArgumentException("Cannot handle enums > 64 constants");
        }
        return new AtomicEnumSetMedium<>(first);
    }

    @SafeVarargs
    public static <E extends Enum<E>> AtomicEnumSet<E> of(E first, E... more) {
        Class<E> type = first.getDeclaringClass();
        E[] consts = type.getEnumConstants();
        if (consts.length < 32) {
            return new AtomicEnumSetSmall<>(first, more);
        }
        if (consts.length > 64) {
            throw new IllegalArgumentException("Cannot handle enums > 64 constants");
        }
        return new AtomicEnumSetMedium<>(first, more);
    }
}
