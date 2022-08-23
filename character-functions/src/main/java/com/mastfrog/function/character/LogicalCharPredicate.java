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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * As usual, loggability wins over lambdas, so, a couple of implementations that
 * do something reasonable for <code>toString()</code>.
 *
 * @author Tim Boudreau
 */
final class LogicalCharPredicate implements CharPredicate {

    private final boolean or;
    private final CharPredicate a;
    private final CharPredicate b;

    public LogicalCharPredicate(boolean or, CharPredicate a, CharPredicate b) {
        this.or = or;
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean test(char c) {
        if (or) {
            return a.test(c) || b.test(c);
        } else {
            return a.test(c) && b.test(c);
        }
    }

    @Override
    public String toString() {
        char sym = or ? '|' : '&';
        return "(" + a + " " + sym + " " + b + ")";
    }

    @Override
    public CharPredicate and(CharPredicate other) {
        if (or) {
            return CharPredicate.super.and(other);
        } else {
            return new LogicalListCharPredicate(false, a, b, other);
        }
    }

    @Override
    public CharPredicate or(CharPredicate other) {
        if (!or) {
            return CharPredicate.super.or(other);
        } else {
            return new LogicalListCharPredicate(true, a, b, other);
        }
    }

    private static class LogicalListCharPredicate implements CharPredicate {

        private final boolean or;
        private final List<CharPredicate> all;

        LogicalListCharPredicate(boolean or, CharPredicate a, CharPredicate b, CharPredicate c) {
            this.or = or;
            all = new ArrayList<>(5);
            all.add(a);
            all.add(b);
            all.add(c);
        }

        @Override
        public boolean test(char c) {
            if (or) {
                for (CharPredicate pred : all) {
                    if (pred.test(c)) {
                        return true;
                    }
                }
                return false;
            } else {
                for (CharPredicate pred : all) {
                    if (!pred.test(c)) {
                        return false;
                    }
                }
                return true;
            }
        }

        @Override
        public String toString() {
            char sym = or ? '|' : '&';
            StringBuilder sb = new StringBuilder('(');
            for (Iterator<CharPredicate> it = all.iterator(); it.hasNext();) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(' ').append(sym).append(' ');
                }
            }
            return sb.append(')').toString();
        }
    }
}
