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

import java.util.function.Function;

/**
 * A predicate for characters; convenience implementations in
 * {@link org.nemesis.charfilter.CharPredicates}.
 *
 * @see org.nemesis.charfilter.CharPredicates
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface CharPredicate {

    public static final CharPredicate EVERYTHING = new AllOrNothing(true);
    public static final CharPredicate NOTHING = new AllOrNothing(false);

    boolean test(char c);

    static CharPredicateBuilder<CharPredicate, ? extends CharPredicateBuilder<CharPredicate, ?>> builder() {
        return CharPredicateBuilder.create();
    }

    static <T> CharPredicateBuilder<T, ? extends CharPredicateBuilder<T, ?>> builder(Function<CharPredicate, T> converter) {
        return CharPredicateBuilder.from(converter);
    }

    static CharPredicate anyOf(char... chars) {
        if (chars.length == 0) {
            return EVERYTHING.negate();
        }
        return new CharsCharPredicate(chars);
    }

    static CharPredicate noneOf(char... chars) {
        if (chars.length == 0) {
            return EVERYTHING.negate();
        }
        return new CharsCharPredicate(chars).negate();
    }

    default CharPredicate and(CharPredicate other) {
        return new LogicalCharPredicate(false, this, other);
    }

    default CharPredicate or(CharPredicate other) {
        return new LogicalCharPredicate(true, this, other);
    }

    default CharPredicate negate() {
        return new NegatedCharPredicate(this);
    }

    static CharPredicate combine(boolean or, CharPredicate... all) {
        if (all == null || all.length == 0) {
            return EVERYTHING;
        }
        CharPredicate curr = all[0];
        for (int i = 1; i < all.length; i++) {
            if (or) {
                curr = curr.or(all[i]);
            } else {
                curr = curr.and(all[i]);
            }
        }
        return curr;
    }
}
