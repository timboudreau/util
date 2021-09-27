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
package com.mastfrog.concurrent.lock;

import com.mastfrog.function.state.Int;
import com.mastfrog.function.state.Lng;
import com.mastfrog.function.state.Obj;
import com.mastfrog.function.throwing.ThrowingIntSupplier;
import com.mastfrog.function.throwing.ThrowingLongSupplier;
import com.mastfrog.function.throwing.ThrowingRunnable;
import com.mastfrog.function.throwing.ThrowingSupplier;
import com.mastfrog.function.throwing.io.IOIntSupplier;
import com.mastfrog.function.throwing.io.IOLongSupplier;
import com.mastfrog.function.throwing.io.IORunnable;
import com.mastfrog.function.throwing.io.IOSupplier;
import com.mastfrog.util.preconditions.Exceptions;
import java.io.IOException;
import java.util.BitSet;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Base interface for slotted locks which defines the required implementation
 * methods and convenience implementations for use with Supplier, LongSupplier
 * and IntSupplier for returning a result from a locking operation, reentrantly
 * and non-reentrantly, where the operation code may throw.
 *
 * @author Tim Boudreau
 */
public interface MultiplyLockable {

    default long slotCount() {
        return 64;
    }

    void lockingThrowing(ThrowingRunnable run, BitSet bits) throws Exception;

    void lockingThrowing(ThrowingRunnable run, int first, int... more) throws Exception;

    void lockingReentrantlyThrowing(ThrowingRunnable run, BitSet bits) throws Exception;

    void lockingReentrantlyThrowing(ThrowingRunnable run, int first, int... more) throws Exception;

//    default void locking(Runnable run, BitSet bits) throws Exception {
//        lockingThrowing(run::run, bits);
//    }
//
//    default void locking(Runnable run, int first, int... more) throws Exception {
//        lockingThrowing(run::run, first, more);
//    }
    default void lockingReentrantlyIO(IORunnable run, BitSet bits) throws InterruptedException, IOException {
        try {
            lockingReentrantlyThrowing(run, bits);
        } catch (Exception ex) {
            Exceptions.chuck(ex);
        }
    }

    default void lockingReentrantlyIO(IORunnable run, int first, int... more) throws IOException {
        try {
            lockingReentrantlyThrowing(run, first, more);
        } catch (Exception ex) {
            Exceptions.chuck(ex);
        }
    }

    default void lockingReentrantly(Runnable run, BitSet slots) throws InterruptedException {
        try {
            lockingReentrantlyThrowing(run::run, slots);
        } catch (Exception ex) {
            Exceptions.chuck(ex);
        }
    }

    default void lockingReentrantly(Runnable run, int first, int... more) throws InterruptedException {
        try {
            lockingReentrantlyThrowing(run::run, first, more);
        } catch (Exception ex) {
            Exceptions.chuck(ex);
        }
    }

    default <T> T getLockingReentrantly(Supplier<T> supp, int first, int... more) throws InterruptedException {
        Obj<T> obj = Obj.create();
        lockingReentrantly(() -> {
            obj.set(supp.get());
        }, first, more);
        return obj.get();
    }

    default <T> T getLockingReentrantlyThrowing(ThrowingSupplier<T> supp, int first, int... more) throws Exception {
        Obj<T> obj = Obj.create();
        lockingReentrantlyThrowing(() -> {
            obj.set(supp.get());
        }, first, more);
        return obj.get();
    }

    default <T> T getLockingReentrantlyIO(IOSupplier<T> supp, int first, int... more) throws InterruptedException, IOException {
        Obj<T> obj = Obj.create();
        try {
            lockingReentrantlyThrowing(() -> {
                obj.set(supp.get());
            }, first, more);
        } catch (Exception ex) {
            return Exceptions.chuck(ex);
        }
        return obj.get();
    }

    default <T> T getLockingReentrantly(Supplier<T> supp, BitSet slots) throws InterruptedException {
        Obj<T> obj = Obj.create();
        lockingReentrantly(() -> {
            obj.set(supp.get());
        }, slots);
        return obj.get();

    }

    default <T> T getLockingReentrantlyThrowing(ThrowingSupplier<T> supp, BitSet slots) throws Exception {
        Obj<T> obj = Obj.create();
        lockingReentrantlyThrowing(() -> {
            obj.set(supp.get());
        }, slots);
        return obj.get();
    }

    default <T> T getLockingReentrantlyIO(IOSupplier<T> supp, BitSet slots) throws InterruptedException, IOException {
        Obj<T> obj = Obj.create();
        try {
            lockingReentrantlyThrowing(() -> {
                obj.set(supp.get());
            }, slots);
        } catch (Exception ex) {
            return Exceptions.chuck(ex);
        }
        return obj.get();
    }

    default long lockingReentrantly(LongSupplier supp, int first, int... more) throws InterruptedException {
        Lng lng = Lng.create();
        lockingReentrantly(() -> {
            lng.set(supp.getAsLong());
        }, first, more);
        return lng.get();
    }

    default int lockingReentrantly(IntSupplier supp, int first, int... more) throws InterruptedException {
        Int in = Int.create();
        lockingReentrantly(() -> {
            in.set(supp.getAsInt());
        }, first, more);
        return in.get();
    }

    default long lockingReentrantlyIO(IOLongSupplier supp, int first, int... more) throws InterruptedException, IOException {
        Lng lng = Lng.create();
        try {
            lockingReentrantlyThrowing(() -> {
                lng.set(supp.getAsLong());
            }, first, more);
        } catch (Exception ex) {
            return Exceptions.chuck(ex);
        }
        return lng.get();
    }

    default int lockingReentrantlyIO(IOIntSupplier supp, int first, int... more) throws InterruptedException, IOException {
        Int in = Int.create();
        try {
            lockingReentrantlyThrowing(() -> {
                in.set(supp.getAsInt());
            }, first, more);
        } catch (Exception ex) {
            return (Integer) Exceptions.chuck(ex);
        }
        return in.get();
    }

    default long lockingReentrantlyThrowing(ThrowingLongSupplier supp, int first, int... more) throws Exception {
        Lng lng = Lng.create();
        lockingReentrantlyThrowing(() -> {
            lng.set(supp.getAsLong());
        }, first, more);
        return lng.get();
    }

    default int lockingReentrantlyThrowing(ThrowingIntSupplier supp, int first, int... more) throws Exception {
        Int in = Int.create();
        try {
            lockingReentrantlyThrowing(() -> {
                in.set(supp.getAsInt());
            }, first, more);
        } catch (Exception ex) {
            return (Integer) Exceptions.chuck(ex);
        }
        return in.get();
    }

    default long lockingReentrantly(LongSupplier supp, BitSet slots) throws InterruptedException {
        Lng lng = Lng.create();
        lockingReentrantly(() -> {
            lng.set(supp.getAsLong());
        }, slots);
        return lng.get();
    }

    default int lockingReentrantly(IntSupplier supp, BitSet slots) throws InterruptedException {
        Int in = Int.create();
        lockingReentrantly(() -> {
            in.set(supp.getAsInt());
        }, slots);
        return in.get();
    }

    default long lockingReentrantlyIO(IOLongSupplier supp, BitSet slots) throws InterruptedException, IOException {
        Lng lng = Lng.create();
        try {
            lockingReentrantlyThrowing(() -> {
                lng.set(supp.getAsLong());
            }, slots);
        } catch (Exception ex) {
            return Exceptions.chuck(ex);
        }
        return lng.get();
    }

    default int lockingReentrantlyIO(IOIntSupplier supp, BitSet slots) throws InterruptedException, IOException {
        Int in = Int.create();
        try {
            lockingReentrantlyThrowing(() -> {
                in.set(supp.getAsInt());
            }, slots);
        } catch (Exception ex) {
            return (Integer) Exceptions.chuck(ex);
        }
        return in.get();
    }

    default long lockingReentrantlyThrowing(ThrowingLongSupplier supp, BitSet slots) throws Exception {
        Lng lng = Lng.create();
        lockingReentrantlyThrowing(() -> {
            lng.set(supp.getAsLong());
        }, slots);
        return lng.get();
    }

    default int lockingReentrantlyThrowing(ThrowingIntSupplier supp, BitSet slots) throws Exception {
        Int in = Int.create();
        try {
            lockingReentrantlyThrowing(() -> {
                in.set(supp.getAsInt());
            }, slots);
        } catch (Exception ex) {
            return (Integer) Exceptions.chuck(ex);
        }
        return in.get();
    }

    default void locking(Runnable run, BitSet slots) throws InterruptedException {
        try {
            lockingThrowing(run::run, slots);
        } catch (Exception ex) {
            Exceptions.chuck(ex);
        }
    }

    default void locking(Runnable run, int first, int... more) throws InterruptedException {
        try {
            lockingThrowing(run::run, first, more);
        } catch (Exception ex) {
            Exceptions.chuck(ex);
        }
    }

    default <T> T getLocking(Supplier<T> supp, int first, int... more) throws InterruptedException {
        Obj<T> obj = Obj.create();
        locking(() -> {
            obj.set(supp.get());
        }, first, more);
        return obj.get();

    }

    default <T> T getLockingThrowing(ThrowingSupplier<T> supp, int first, int... more) throws Exception {
        Obj<T> obj = Obj.create();
        lockingThrowing(() -> {
            obj.set(supp.get());
        }, first, more);
        return obj.get();
    }

    default <T> T getLockingIO(IOSupplier<T> supp, int first, int... more) throws InterruptedException, IOException {
        Obj<T> obj = Obj.create();
        try {
            lockingThrowing(() -> {
                obj.set(supp.get());
            }, first, more);
        } catch (Exception ex) {
            return Exceptions.chuck(ex);
        }
        return obj.get();
    }

    default <T> T getLocking(Supplier<T> supp, BitSet slots) throws InterruptedException {
        Obj<T> obj = Obj.create();
        locking(() -> {
            obj.set(supp.get());
        }, slots);
        return obj.get();

    }

    default <T> T getLockingThrowing(ThrowingSupplier<T> supp, BitSet slots) throws Exception {
        Obj<T> obj = Obj.create();
        lockingThrowing(() -> {
            obj.set(supp.get());
        }, slots);
        return obj.get();
    }

    default <T> T getLockingIO(IOSupplier<T> supp, BitSet slots) throws InterruptedException, IOException {
        Obj<T> obj = Obj.create();
        try {
            lockingThrowing(() -> {
                obj.set(supp.get());
            }, slots);
        } catch (Exception ex) {
            return Exceptions.chuck(ex);
        }
        return obj.get();
    }

    default long locking(LongSupplier supp, int first, int... more) throws InterruptedException {
        Lng lng = Lng.create();
        locking(() -> {
            lng.set(supp.getAsLong());
        }, first, more);
        return lng.get();
    }

    default int locking(IntSupplier supp, int first, int... more) throws InterruptedException {
        Int in = Int.create();
        locking(() -> {
            in.set(supp.getAsInt());
        }, first, more);
        return in.get();
    }

    default long lockingIO(IOLongSupplier supp, int first, int... more) throws InterruptedException, IOException {
        Lng lng = Lng.create();
        try {
            lockingThrowing(() -> {
                lng.set(supp.getAsLong());
            }, first, more);
        } catch (Exception ex) {
            return Exceptions.chuck(ex);
        }
        return lng.get();
    }

    default int lockingIO(IOIntSupplier supp, int first, int... more) throws InterruptedException, IOException {
        Int in = Int.create();
        try {
            lockingThrowing(() -> {
                in.set(supp.getAsInt());
            }, first, more);
        } catch (Exception ex) {
            return (Integer) Exceptions.chuck(ex);
        }
        return in.get();
    }

    default long lockingThrowing(ThrowingLongSupplier supp, int first, int... more) throws Exception {
        Lng lng = Lng.create();
        lockingThrowing(() -> {
            lng.set(supp.getAsLong());
        }, first, more);
        return lng.get();
    }

    default int lockingThrowing(ThrowingIntSupplier supp, int first, int... more) throws Exception {
        Int in = Int.create();
        try {
            lockingThrowing(() -> {
                in.set(supp.getAsInt());
            }, first, more);
        } catch (Exception ex) {
            return (Integer) Exceptions.chuck(ex);
        }
        return in.get();
    }

    default long locking(LongSupplier supp, BitSet slots) throws InterruptedException {
        Lng lng = Lng.create();
        locking(() -> {
            lng.set(supp.getAsLong());
        }, slots);
        return lng.get();
    }

    default int locking(IntSupplier supp, BitSet slots) throws InterruptedException {
        Int in = Int.create();
        locking(() -> {
            in.set(supp.getAsInt());
        }, slots);
        return in.get();
    }

    default long lockingIO(IOLongSupplier supp, BitSet slots) throws InterruptedException, IOException {
        Lng lng = Lng.create();
        try {
            lockingThrowing(() -> {
                lng.set(supp.getAsLong());
            }, slots);
        } catch (Exception ex) {
            return Exceptions.chuck(ex);
        }
        return lng.get();
    }

    default int lockingIO(IOIntSupplier supp, BitSet slots) throws InterruptedException, IOException {
        Int in = Int.create();
        try {
            lockingThrowing(() -> {
                in.set(supp.getAsInt());
            }, slots);
        } catch (Exception ex) {
            return (Integer) Exceptions.chuck(ex);
        }
        return in.get();
    }

    default long lockingThrowing(ThrowingLongSupplier supp, BitSet slots) throws Exception {
        Lng lng = Lng.create();
        lockingThrowing(() -> {
            lng.set(supp.getAsLong());
        }, slots);
        return lng.get();
    }

    default int lockingThrowing(ThrowingIntSupplier supp, BitSet slots) throws Exception {
        Int in = Int.create();
        try {
            lockingThrowing(() -> {
                in.set(supp.getAsInt());
            }, slots);
        } catch (Exception ex) {
            return (Integer) Exceptions.chuck(ex);
        }
        return in.get();
    }
}
