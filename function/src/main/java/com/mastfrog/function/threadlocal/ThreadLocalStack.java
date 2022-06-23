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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Wrapper for a thread-local stack of re-entered objects or similar, with the
 * quirk that an element which is already the head element will not be added
 * twice.
 *
 * @author Tim Boudreau
 */
public final class ThreadLocalStack<T> implements Iterable<T>
{
    private final ThreadLocalValue<LinkedList<T>> value
            = ThreadLocalValue.create(LinkedList::new);

    private ThreadLocalStack()
    {

    }

    /**
     * Create a new stack.
     *
     * @param <T> T the type
     * @return A ThreadLocalStack
     */
    public static <T> ThreadLocalStack<T> create()
    {
        return new ThreadLocalStack<>();
    }

    /**
     * Create a duplicate of the stack contents.
     *
     * @return A list
     */
    public List<T> copy()
    {
        List<T> result = new ArrayList<>(value.get());
        Collections.reverse(result);
        return result;
    }

    /**
     * Determine if the stack is empty
     *
     * @return true if it is empty
     */
    public boolean isEmpty()
    {
        return value.get().isEmpty();
    }

    /**
     * Get the head element if there is one
     *
     * @return The head element if present
     */
    public Optional<T> head()
    {
        LinkedList<T> l = value.get();
        return l.isEmpty()
               ? Optional.empty()
               : Optional.ofNullable(l.getFirst());
    }

    /**
     * Get the head element if there is one
     *
     * @return The head element if present
     */
    public Optional<T> tail()
    {
        LinkedList<T> l = value.get();
        return l.isEmpty()
               ? Optional.empty()
               : Optional.ofNullable(l.getFirst());
    }

    @Override
    public Iterator<T> iterator()
    {
        return copy().iterator();
    }

    /**
     * Push an element onto the stack (if it is not already the head), do some
     * work, then pop it.
     *
     * @param obj An element to push onto the stack
     * @param run the work
     */
    public void pushing(T obj, Runnable run)
    {
        LinkedList<T> items = value.get();
        T old = items.isEmpty()
                ? null
                : items.getLast();
        if (old == obj)
        {
            run.run();
        }
        else
        {
            value.get().push(obj);
            try
            {
                run.run();
            }
            finally
            {
                value.get().pop();
            }
        }
    }

    /**
     * Push an element onto the stack (if it is not already the head), do some
     * work, then pop it.
     *
     * @param obj An element to push onto the stack
     * @param run the work
     */
    public void pushingThrowing(T obj, ThrowingRunnable run)
    {
        pushing(obj, run.toNonThrowing());
    }

    /**
     * Push an element onto the stack (if it is not already the head), do some
     * work, then pop it.
     *
     * @param obj An element to push onto the stack
     * @param supp the work
     * @return the return value of the supplier
     */
    public <R> R pushing(T obj, Supplier<R> supp)
    {
        LinkedList<T> items = value.get();
        T old = items.isEmpty()
                ? null
                : items.getLast();
        if (old == obj)
        {
            return supp.get();
        }
        else
        {
            items.push(obj);
            try
            {
                return supp.get();
            }
            finally
            {
                value.get().pop();
            }
        }
    }

    /**
     * Push an element onto the stack (if it is not already the head), do some
     * work, then pop it.
     *
     * @param obj An element to push onto the stack
     * @param supp the work
     * @return the return value of the supplier
     */
    public <R> R pushingThrowing(T obj, ThrowingSupplier<R> run)
    {
        return pushing(obj, run.asSupplier());
    }

    @Override
    public String toString()
    {
        return value.get().toString();
    }

    /**
     * Push an item, returning a QuietAutoClosable whose close() method will
     * remove the pushed item.
     *
     * @param obj An object to push onto the stack
     * @return
     */
    public QuietAutoClosable push(T obj)
    {
        LinkedList<T> items = value.get();
        T old = items.isEmpty()
                ? null
                : items.getLast();
        if (old == obj)
        {
            return QuietAutoClosable.NO_OP;
        }
        else
        {
            items.push(obj);
            return () -> items.pop();
        }
    }
}
