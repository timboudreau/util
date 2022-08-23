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

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A builder for a CharFilter which delegates to two CharPredicateBuilders, one
 * for the initial character and one for the subsequent ones.
 *
 * @see org.nemesis.charfilter.CharFilter
 * @author Tim Boudreau
 */
public final class CharFilterBuilder<T> {

    private final Function<? super CharFilter, T> converter;

    CharFilterBuilder(Function<? super CharFilter, T> converter) {
        this.converter = converter;
    }

    static <T> CharFilterBuilder<T> from(Function<? super CharFilter, T> converter) {
        return new CharFilterBuilder<>(converter);
    }

    static CharFilterBuilder<CharFilter> create() {
        return CharFilterBuilder.from(filter -> filter);
    }

    /**
     * Use the passed predicate as the only one and move on to the subsequent
     * characters builder.
     *
     * @param predicate A predicate
     * @return a builder that can be completed
     */
    public FinishableCharFilterBuilder<T> testInitialCharacterWith(CharPredicate predicate) {
        return new FinishableCharFilterBuilder<>(predicate, converter);
    }

    /**
     * The passed consumer is passed a builder for constructing those tests
     * applied to the initial character by the resulting filter, and returns a
     * builder for the subsequent characters.
     *
     * @param predicate A consumer
     * @return a builder that can be completed
     */
    public FinishableCharFilterBuilder<T> testInitialCharacterWith(Consumer<CharPredicateBuilder<?, ? extends CharPredicateBuilder<?, ?>>> c) {
        Holder<FinishableCharFilterBuilder<T>> holder = new Holder<>();
        Function<CharPredicate, Void> f = pred -> {
            FinishableCharFilterBuilder<T> result = new FinishableCharFilterBuilder<>(pred, converter);
            holder.set(result);
            return null;
        };
        CharPredicateBuilder<Void, ? extends CharPredicateBuilder<Void, ?>> cb = CharPredicateBuilder.from(f);
        c.accept(cb);
        return holder.get(cb::build);
    }

    /**
     * Returns a builder for constructing those tests applied to the initial
     * character by the resulting filter, whose <code>build()</code> method then
     * returns a builder for the subsequent characters.
     *
     * @param predicate A consumer
     * @return a builder that can be completed
     */
    public CharPredicateBuilder<FinishableCharFilterBuilder<T>, ? extends CharPredicateBuilder<FinishableCharFilterBuilder<T>, ?>> testInitialCharacterWith() {
        Function<CharPredicate, FinishableCharFilterBuilder<T>> f = pred -> {
            return new FinishableCharFilterBuilder<>(pred, converter);
        };
        return CharPredicateBuilder.from(f);
    }

    /**
     * Finish this builder using a single predicate for initial and later
     * characters.
     *
     * @param predicate A predicate
     * @return the object this builder builds, determined at construction time
     */
    public T testingAllCharactersWith(CharPredicate predicate) {
        return converter.apply(CharFilter.of(predicate));
    }

    public static class FinishableCharFilterBuilder<T> {

        private final CharPredicate initial;
        private final Function<? super CharFilter, T> converter;

        public FinishableCharFilterBuilder(CharPredicate initial, Function<? super CharFilter, T> converter) {
            this.initial = initial;
            this.converter = converter;
        }

        private CharFilter create(CharPredicate subsequent) {
            return CharFilter.of(initial == null ? CharPredicate.EVERYTHING : initial,
                    subsequent == null ? CharPredicate.EVERYTHING : subsequent);
        }

        public CharPredicateBuilder<T, ? extends CharPredicateBuilder<T, ?>> testSubsequentCharacterWith() {
            return CharPredicateBuilder.from(pred -> {
                return converter.apply(create(pred));
            });
        }

        public T testSubsequentCharacterWith(Consumer<CharPredicateBuilder<?, ? extends CharPredicateBuilder<?, ?>>> c) {
            Holder<T> holder = new Holder<>();
            Function<CharPredicate, Void> f = pred -> {
                holder.set(converter.apply(create(pred)));
                return null;
            };
            CharPredicateBuilder<Void, ? extends CharPredicateBuilder<Void, ?>> cb = CharPredicateBuilder.from(f);
            c.accept(cb);
            return holder.get(cb::build);
        }
    }
}
