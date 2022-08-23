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

import static com.mastfrog.function.character.CharPredicate.EVERYTHING;
import java.util.function.Function;

/**
 * A predicate for strings and characters which distinguishes the initial
 * character as possibly being subject to different tests than all subsequent
 * characters.
 *
 * @author Tim Boudreau
 */
public interface CharFilter {

    boolean test(boolean isInitial, char typed);

    public static final CharFilter ALL = new AllOrNothing(true);
    public static final CharFilter NONE = new AllOrNothing(false);

    public static CharFilterBuilder<CharFilter> builder() {
        return CharFilterBuilder.create();
    }

    public static <T> CharFilterBuilder<T> builder(Function<? super CharFilter, T> converter) {
        return CharFilterBuilder.from(converter);
    }

    default boolean test(CharSequence string) {
        return test(string, false);
    }

    default boolean test(CharSequence string, boolean allowEmpty) {
        if (string == null) {
            return allowEmpty;
        }
        int max = string.length();
        for (int i = 0; i < max; i++) {
            if (!test(i == 0, string.charAt(i))) {
                return false;
            }
        }
        return !allowEmpty ? max > 0 : true;
    }

    public static CharFilter of(CharPredicate all) {
        return CharFilter.of(all, all);
    }

    public static CharFilter of(CharPredicate initial, CharPredicate subsequent) {
        return new PredicatesCharFilter(initial, subsequent);
    }

    public static CharFilter of(CharPredicate[] initial, CharPredicate[] subsequent) {
        CharPredicate i = initial.length == 0 ? EVERYTHING : initial[0];
        for (int j = 1; j < initial.length; j++) {
            i = i.or(initial[j]);
        }
        CharPredicate s = subsequent.length == 0 ? EVERYTHING : subsequent[0];
        for (int j = 1; j < subsequent.length; j++) {
            s = s.or(subsequent[j]);
        }
        return CharFilter.of(i, s);
    }
}
