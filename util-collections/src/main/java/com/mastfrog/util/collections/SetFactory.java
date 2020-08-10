/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.util.collections;

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A factory for sets.
 *
 * @author Tim Boudreau
 */
public interface SetFactory<V> {

    /**
     * Create a new supplier of sets with the passed characteristics.
     *
     * @param <T> The value type
     * @param initialCapacity The initial capacity of created sets
     * @param threadSafe If true, the result should be concurrent or
     * synchronized
     * @return A supplier of sets
     */
    <T extends V> Supplier<Set<T>> setSupplier(int initialCapacity, boolean threadSafe);

    /**
     * Create a set.
     *
     * @param <T> The set member type
     * @param initialCapacity The initial capacity
     * @param threadSafe If true, return a concurrent or synchronized
     * implementation
     * @return A set
     */
    default <T extends V> Set<T> newSet(int initialCapacity, boolean threadSafe) {
        return this.<T>setSupplier(initialCapacity, threadSafe).get();
    }

    default <T extends V> Set<T> ofCollection(Collection<? extends T> objs) {
        return ofCollection(false, objs);
    }

    default <T extends V> Set<T> ofCollection(boolean threadSafe, Collection<? extends T> objs) {
        Set<T> result = newSet(notNull("objs", objs).size(), threadSafe);
        result.addAll(objs);
        return result;
    }

    default <T extends V> Set<T> of(T... objs) {
        return of(false, objs);
    }

    default <T extends V> Set<T> of(boolean threadSafe, T... objs) {
        Set<T> result = newSet(objs.length, threadSafe);
        if (objs.length > 0) {
            for (T obj : objs) {
                result.add(obj);
            }
        }
        return result;
    }

}
