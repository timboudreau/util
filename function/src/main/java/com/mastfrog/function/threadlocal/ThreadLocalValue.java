////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// © 2011-2022 Telenav, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
package com.mastfrog.function.threadlocal;

import com.mastfrog.function.misc.QuietAutoClosable;
import com.mastfrog.function.throwing.ThrowingRunnable;
import com.mastfrog.function.throwing.ThrowingSupplier;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.mastfrog.util.preconditions.Checks.notNull;

/**
 * Wraps a ThreadLocal and provides convenient methods set it, pass a closure
 * which executes, after which it is reset to its previous values. Useful for
 * tracking reentrant things and restoring context when they exit.
 *
 * @see ThreadLocal
 * @author Tim Boudreau
 */
public final class ThreadLocalValue<T>
{
    private final ThreadLocal<T> local;

    private ThreadLocalValue(ThreadLocal<T> local)
    {
        this.local = local;
    }

    private ThreadLocalValue(Supplier<T> supp)
    {
        this(ThreadLocal.withInitial(supp));
    }

    private ThreadLocalValue()
    {
        this(new ThreadLocal<>());
    }

    /**
     * Create a new ThreadLocalValue.
     *
     * @param <T> The type
     * @return A ThreadLocalValue
     */
    public static <T> ThreadLocalValue<T> create()
    {
        return new ThreadLocalValue<>();
    }

    /**
     * Create a new ThreadLocalValue.
     *
     * @param <T> The type
     * @param supp A supplier for the initial value
     * @return A new ThreadLocalValue
     * @see ThreadLocal#withInitial(java.util.function.Supplier)
     */
    public static <T> ThreadLocalValue<T> create(Supplier<T> supp)
    {
        return new ThreadLocalValue<>(supp);
    }

    /**
     * Create a new ThreadLocalValue with an initial value for use when the
     * value has not been explicitly set.
     *
     * @param <T> The type
     * @param initialValue The value to use when not explicitly set
     * @return a new ThreadLocalValue
     * @see ThreadLocal#withInitial(java.util.function.Supplier)
     */
    public static <T> ThreadLocalValue<T> create(T initialValue)
    {
        return new ThreadLocalValue<>(() -> initialValue);
    }

    /**
     * Create a new ThreadLocalValue using an existing THreadLocal.
     *
     * @param <T> The type
     * @param using The ThreadLocal
     * @return a new ThreadLocalValue
     */
    public static <T> ThreadLocalValue createWith(ThreadLocal<T> using)
    {
        return new ThreadLocalValue<>(notNull("using", using));
    }

    /**
     * Allows the value to be set and cleared using the try-with-resources
     * pattern.
     *
     * @param value A value, not null
     * @return An AutoClosable
     */
    public QuietAutoClosable setTo(T value)
    {
        return new AutoCloseableImpl(set(value));
    }

    /**
     * Pass the value to the passed consumer if non-null.
     *
     * @param val The value
     * @return Whether or not the consumer was called
     */
    public boolean usingValue(Consumer<T> val)
    {
        T obj = local.get();
        if (obj != null)
        {
            val.accept(obj);
            return true;
        }
        return false;
    }

    /**
     * Set the value to the passed value, execute the runnable, and reset it to
     * whatever it was before this call.
     *
     * @param obj An object
     * @param run A runnable
     */
    public void withValue(T obj, Runnable run)
    {
        T old = local.get();
        local.set(obj);
        try
        {
            notNull("run", run).run();
        }
        finally
        {
            local.set(old);
        }
    }

    /**
     * Set the value to the passed value, execute the runnable, and reset it to
     * whatever it was before this call.
     *
     * @param obj An object
     * @param run A runnable
     */
    public void withValueThrowing(T obj, ThrowingRunnable tr)
    {
        withValue(obj, tr.toNonThrowing());
    }

    /**
     * Set the value to the passed value, execute the runnable, and reset it to
     * whatever it was before this call.
     *
     * @param obj An object
     * @param supp the supplier of the return value
     */
    public <R> R withValueThrowing(T obj, ThrowingSupplier<R> supp)
    {
        return withValue(obj, supp.asSupplier());
    }

    /**
     * Set the value to the passed value, execute the runnable, and reset it to
     * whatever it was before this call.
     *
     * @param obj An object
     * @param supp the supplier of the return value
     */
    public <R> R withValue(T obj, Supplier<R> supp)
    {
        T old = local.get();
        local.set(obj);
        try
        {
            return supp.get();
        }
        finally
        {
            local.set(old);
        }
    }

    /**
     * Get the current value.
     *
     * @return The current value
     */
    public T get()
    {
        return local.get();
    }

    /**
     * Clear the current value, returning the old value.
     *
     * @return The old value if present
     */
    public Optional<T> clear()
    {
        T obj = local.get();
        local.remove();
        return Optional.ofNullable(obj);
    }

    /**
     * Set the value, returning the previous value.
     *
     * @param obj
     * @return The old value or null
     */
    public T set(T obj)
    {
        T old = local.get();
        local.set(obj);
        return old;
    }

    /**
     * Get the current value as an Optional.
     *
     * @return The current value as an optional
     */
    public Optional<T> toOptional()
    {
        return Optional.ofNullable(local.get());
    }

    final class AutoCloseableImpl implements QuietAutoClosable
    {
        private final T oldValue;

        AutoCloseableImpl(T oldValue)
        {
            this.oldValue = oldValue;
        }

        @Override
        public void close()
        {
            local.set(oldValue);
        }
    }
}
