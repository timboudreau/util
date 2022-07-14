/*
 * Copyright 2016-2020 Tim Boudreau, Frédéric Yvon Vinet
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
package com.mastfrog.concurrent.coalesce;

import com.mastfrog.function.throwing.ThrowingSupplier;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A wrapper around a WorkCoalescer which owns the reference to the data, and
 * can optionally test if the cached value is still valid, and can recreate it.
 * <p>
 * A cached, lazily computed value which can recompute on demand and which
 * guarantees only one thread does the recomputation, so the view to callers is
 * always consistent.
 * </p>
 *
 * @author Tim Boudreau
 */
public class Coalescer<T> implements ThrowingSupplier<T> {

    private final WorkCoalescer<T> wc;
    private final AtomicReference<T> ref = new AtomicReference<>();
    private final Predicate<T> expiredTest;
    private final Supplier<T> valueSupplier;

    public Coalescer(Predicate<T> expiredTest, Supplier<T> valueSupplier) {
        this("unnamed", expiredTest, valueSupplier);
    }

    public Coalescer(Supplier<T> valueSupplier) {
        this("unnamed", valueSupplier);
    }

    public Coalescer(String name, Supplier<T> valueSupplier) {
        this(name, old -> false, valueSupplier);
    }

    public Coalescer(String name, Predicate<T> expiredTest, Supplier<T> valueSupplier) {
        wc = new WorkCoalescer<>(name);
        this.expiredTest = expiredTest;
        this.valueSupplier = valueSupplier;
    }

    public T current() {
        return ref.get();
    }

    @Override
    public T get() throws InterruptedException {
        return wc.coalesceComputation(() -> {
            T old = ref.get();
            if (old != null && !expiredTest.test(old)) {
                return old;
            }
            return valueSupplier.get();
        }, ref);
    }
}
