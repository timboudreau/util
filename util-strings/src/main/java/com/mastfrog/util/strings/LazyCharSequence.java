/*
 * Copyright 2019 Mastfrog Technologies.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastfrog.util.strings;

import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * A lazily computed character sequence using a Supplier, so error detail
 * messages, which are only computed if the user hovers over an error annotation
 * or presses alt enter on it, are not computed and dragged around until and
 * unless they are actually needed.
 *
 * @author Tim Boudreau
 */
final class LazyCharSequence implements CharSequence {

    private final Supplier<? extends CharSequence> delegateSupplier;
    private CharSequence delegate;
    private final boolean cache;

    LazyCharSequence(Supplier<? extends CharSequence> delegateSupplier, boolean cache) {
        this.delegateSupplier = delegateSupplier;
        this.cache = cache;
    }

    CharSequence delegate() {
        if (!cache) {
            return delegateSupplier.get();
        }
        if (delegate == null) {
            delegate = delegateSupplier.get();
            if (delegate == null) {
                delegate = "";
            }
        }
        return delegate;
    }

    @Override
    public int length() {
        return delegate().length();
    }

    @Override
    public char charAt(int index) {
        return delegate().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new LazyCharSequence(() -> {
            return delegate.subSequence(start, end);
        }, cache);
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

    @Override
    public IntStream chars() {
        return delegate().chars();
    }

    @Override
    public IntStream codePoints() {
        return delegate().codePoints();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        }
        return o instanceof CharSequence && ((CharSequence) o).toString().equals(toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
