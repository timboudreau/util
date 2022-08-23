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
 * A builder for character predicates which allows for composition and logical
 * groupings.
 *
 * @author Tim Boudreau
 */
public class CharPredicateBuilder<T, B extends CharPredicateBuilder<T, B>> {

    private final Function<? super CharPredicate, T> converter;
    protected CharPredicate predicate;

    CharPredicateBuilder(Function<? super CharPredicate, T> converter) {
        this.converter = converter;
    }

    static <T> CharPredicateBuilder<T, ? extends CharPredicateBuilder<T, ?>> from(Function<? super CharPredicate, T> converter) {
        return new CharPredicateBuilder<>(converter);
    }

    static CharPredicateBuilder<CharPredicate, ? extends CharPredicateBuilder<CharPredicate, ?>> create() {
        return new CharPredicateBuilder<>(pred -> pred);
    }

    private CharPredicate toPredicate() {
        if (predicate == null) {
            return CharPredicate.EVERYTHING;
        }
        return predicate;
    }

    /**
     * Accept any of the passed characters, or'ing the resulting predicate with
     * any already provided.
     *
     * @param chars Some characters
     * @return this
     */
    public B include(char... chars) {
        if (chars.length == 0) {
            return cast();
        }
        return include(new CharsCharPredicate(chars));
    }

    /**
     * Accept any of the passed characters, and'ing the negation of the
     * resulting predicate with any already provided.
     *
     * @param chars Some characters
     * @return this
     */
    public B exclude(char... chars) {
        if (chars.length == 0) {
            return cast();
        }
        return exclude(new CharsCharPredicate(chars));
    }

    /**
     * Include the logical OR of all the passed predicates.
     *
     * @param predicates Some predicates
     * @return
     */
    public B includeAny(CharPredicate... predicates) {
        if (predicates.length == 0) {
            return cast();
        }
        return include(CharPredicate.combine(true, predicates));
    }

    /**
     * Include the passed predicate, logically OR'ing it with any already
     * provided.
     *
     * @param predicate A predicate
     * @return this
     */
    public B include(CharPredicate predicate) {
        if (this.predicate == null) {
            this.predicate = predicate;
        } else {
            this.predicate = this.predicate.or(predicate);
        }
        return cast();
    }

    /**
     * Include the passed predicate, logically AND'ing any current predicate
     * with the negation of the passed one.
     *
     * @param predicate A predicate
     * @return this
     */
    public B exclude(CharPredicate predicate) {
        if (this.predicate == null) {
            this.predicate = predicate.negate();
        } else {
            this.predicate = this.predicate.and(predicate.negate());
        }
        return cast();
    }

    public B and(CharPredicate predicate) {
        if (this.predicate != null) {
            this.predicate = this.predicate.and(predicate);
        } else {
            this.predicate = predicate;
        }
        return cast();
    }

    public B or(CharPredicate predicate) {
        if (this.predicate != null) {
            this.predicate = this.predicate.or(predicate);
        } else {
            this.predicate = predicate;
        }
        return cast();
    }

    @SuppressWarnings("unchecked")
    B cast() {
        return (B) this;
    }

    /**
     * Build the object this builder builds.
     *
     * @return An object, type determined at construction time
     */
    public T build() {
        return converter.apply(toPredicate());
    }

    /**
     * Returns a builder for a sub-group of predicates, the composition of which
     * will be logically and'd with any existing predicate.
     *
     * @return A logical group builder
     */
    public GroupCharPredicateBuilder<T, B, ? extends GroupCharPredicateBuilder<T, B, ?>> and() {
        return group(false);
    }

    /**
     * Returns a builder for a sub-group of predicates, the composition of which
     * will be logically or'd with any existing predicate.
     *
     * @return A logical group builder
     */
    public GroupCharPredicateBuilder<T, B, ? extends GroupCharPredicateBuilder<T, B, ?>> or() {
        return group(true);
    }

    /**
     * Passes a builder for a sub-group of predicates to the passed consumer
     * (which need not call <code>build()</code> on it, but must complete its
     * work within the closure of this call), which will be logically or'd with
     * any existing predicate.
     *
     * @param c A consumer
     * @return this
     */
    public B or(Consumer<GroupCharPredicateBuilder<T, B, ? extends GroupCharPredicateBuilder<T, B, ?>>> c) {
        return group(true, c);
    }

    /**
     * Passes a builder for a sub-group of predicates to the passed consumer
     * (which need not call <code>build()</code> on it, but must complete its
     * work within the closure of this call), which will be logically and'd with
     * any existing predicate.
     *
     * @param c A consumer
     * @return this
     */
    public B and(Consumer<GroupCharPredicateBuilder<T, B, ? extends GroupCharPredicateBuilder<T, B, ?>>> c) {
        return group(false, c);
    }

    private B group(boolean or, Consumer<? super GroupCharPredicateBuilder<T, B, ? extends GroupCharPredicateBuilder<T, B, ?>>> c) {
        Holder<B> holder = new Holder<>();
        GroupCharPredicateBuilder<T, B, ? extends GroupCharPredicateBuilder<T, B, ?>> result
                = new GroupCharPredicateBuilder<>(gcpb -> {
                    if (predicate == null) {
                        predicate = gcpb;
                    } else if (or) {
                        predicate = predicate.or(gcpb);
                    } else {
                        predicate = predicate.and(gcpb);
                    }
                    return holder.set(cast());
                });
        c.accept(result);
        return holder.get(() -> {
            result.build();
        });
    }

    private GroupCharPredicateBuilder<T, B, ? extends GroupCharPredicateBuilder<T, B, ?>> group(boolean or) {
        return new GroupCharPredicateBuilder<>(gcpb -> {
            if (predicate == null) {
                predicate = gcpb;
            } else if (or) {
                predicate = predicate.or(gcpb);
            } else {
                predicate = predicate.and(gcpb);
            }
            return cast();
        });
    }

    /**
     * Builder subtype for groups.
     *
     * @param <X> The parent builder result type
     * @param <T> This builder's result type
     * @param <B> The type of this builder
     */
    public static final class GroupCharPredicateBuilder<X, T extends CharPredicateBuilder<X, T>, B extends GroupCharPredicateBuilder<X, T, B>> extends
            CharPredicateBuilder<T, B> {

        // XXX figure out the generics incantation that makes exposing / having this type unnecessary
        GroupCharPredicateBuilder(Function<? super CharPredicate, T> converter) {
            super(converter);
        }
    }

}
