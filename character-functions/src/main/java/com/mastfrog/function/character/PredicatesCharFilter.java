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

/**
 *
 * @author Tim Boudreau
 */
final class PredicatesCharFilter implements CharFilter {

    private final CharPredicate initial;
    private final CharPredicate subsequent;

    PredicatesCharFilter(CharPredicate initial, CharPredicate subsequent) {
        this.initial = initial;
        this.subsequent = subsequent;
    }

    @Override
    public boolean test(boolean isInitial, char typed) {
        if (isInitial) {
            return initial.test(typed);
        } else {
            return subsequent.test(typed);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + initial + " / "
                + subsequent + ")";
    }
}
