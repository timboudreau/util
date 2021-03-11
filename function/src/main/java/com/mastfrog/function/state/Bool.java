/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.function.state;

import com.mastfrog.function.BooleanConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Holder for a primitive boolean that needs to be updated within a lambda
 * or set of recursive calls to hold transitive state.
 *
 * @author Tim Boudreau
 */
public interface Bool extends BooleanConsumer, BooleanSupplier, Supplier<Boolean>, Consumer<Boolean> {

    public static Bool create() {
        return create(false);
    }

    public static Bool create(boolean val) {
        return new BoolImpl(val);
    }

    public static Bool createAtomic() {
        return new BoolAtomic();
    }

    public static Bool createAtomic(boolean val) {
        return new BoolAtomic(val);
    }

    default boolean or(boolean val) {
        boolean old = getAsBoolean();
        set(old || val);
        return old != val;
    }

    default boolean and(boolean val) {
        boolean old = getAsBoolean();
        boolean newVal = old && val;
        if (newVal != old) {
            set(newVal);
            return true;
        }
        return false;
    }

    default boolean xor(boolean val) {
        boolean old = getAsBoolean();
        boolean nue = old ^ val;
        if (nue != old) {
            set(nue);
            return true;
        }
        return false;
    }

    default BooleanSupplier or(BooleanSupplier other) {
        return () -> getAsBoolean() || other.getAsBoolean();
    }

    default BooleanSupplier and(BooleanSupplier other) {
        return () -> getAsBoolean() && other.getAsBoolean();
    }

    default BooleanSupplier xor(BooleanSupplier other) {
        return () -> getAsBoolean() ^ other.getAsBoolean();
    }

    default boolean set() {
        return set(true);
    }

    default boolean reset() {
        return set(false);
    }

    default boolean runAndSet(Runnable r) {
        if (!getAsBoolean()) {
            r.run();
            set();
            return true;
        }
        return false;
    }

    default boolean ifTrue(Runnable r) {
        if (getAsBoolean()) {
            r.run();
            return true;
        }
        return false;
    }

    default boolean ifUntrue(Runnable r) {
        if (!getAsBoolean()) {
            r.run();
            return true;
        }
        return false;
    }

    default boolean set(boolean val) {
        boolean old = get();
        if (old != val) {
            accept(val);
        }
        return old;
    }

    default boolean toggle() {
        return set(!getAsBoolean());
    }

    @Override
    default Boolean get() {
        return getAsBoolean();
    }

    @Override
    default void accept(Boolean t) {
        accept(t.booleanValue());
    }
}
