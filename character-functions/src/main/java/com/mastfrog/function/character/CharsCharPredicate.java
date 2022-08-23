/*
 * Copyright 2016-2019 Tim Boudreau, Frédéric Yvon Vinet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastfrog.function.character;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Predicate for an array of characters.
 *
 * @author Tim Boudreau
 */
final class CharsCharPredicate implements CharPredicate {

    private final char[] chars;

    CharsCharPredicate(char[] chars) {
        Arrays.sort(chars);
        this.chars = chars;
        // XXX check duplicates?  Newer JDKs behave badly on binary search
        // with duplicates.
    }

    @Override
    public boolean test(char c) {
        return Arrays.binarySearch(chars, c) >= 0;
    }

    @Override
    public String toString() {
        return "anyOf(" + new String(chars) + ")";
    }

    @Override
    public CharPredicate or(CharPredicate other) {
        if (other instanceof CharsCharPredicate) {
            CharsCharPredicate oc = (CharsCharPredicate) other;
            Set<Character> all = new TreeSet<>();
            for (char c : chars) {
                all.add(c);
            }
            for (char c : oc.chars) {
                all.add(c);
            }
            char[] items = new char[all.size()];
            Iterator<Character> it = all.iterator();
            int ix = 0;
            while (it.hasNext()) {
                items[ix++] = it.next();
            }
            return new CharsCharPredicate(items);
        }
        return CharPredicate.super.or(other);
    }

}
