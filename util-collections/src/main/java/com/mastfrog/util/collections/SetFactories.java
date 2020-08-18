/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.util.collections;

import java.lang.ref.Reference;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Enum of factories for common set types.
 *
 * @author Tim Boudreau
 */
public enum SetFactories implements SetFactory<Object> {

    /**
     * LinkedHashSet or similar.
     */
    ORDERED_HASH,
    /**
     * Uses a weak-valued hash map.
     */
    WEAK_HASH,
    /**
     * Uses a concurrent, strongly referencing HashMap.
     */
    CONCURRENT_EQUALITY,
    /**
     * Uses a TreeSet comparing on <code>System.identityHashCode()</code> to
     * create effectively an "identity hash set" that can contain two objects
     * which <code>equals()</code> each other.
     */
    IDENTITY,
    /**
     * Uses a ConcurrentSkipListSet with the same trick.
     */
    CONCURRENT_IDENTITY,
    /**
     * Uses an ordinary <code>java.util.HashSet</code> or equivalent.
     */
    UNORDERED_HASH;
    private static final ToIntFunction<Object> IDENTITY_HASH_CODE = o -> o == null ? 0 : System.identityHashCode(o);
    private static final ToIntFunction<Object> OBJECT_HASH_CODE = o -> o == null ? 0 : o.hashCode();

    /**
     * Create a weak (or soft, or something else) hash set with full control
     * over the creation and handling of the references used (for example
     * SoftReferences, or WeakReferences that remain strongly referenced until a
     * timeout, or references attached to your own reference queue.
     *
     * @param <T> The type
     * @param hasher A hashing function (such as <code>Object::hashCode</code>
     * or <code>System::identityHashCode</code>) - it needs to handle null
     * values and values of unexpected types, since the contract of Set allows
     * these to be passed to methods such as <code>contains()</code> - by
     * default, return 0 for null)
     * @param referenceFactory The function which creates reference objects
     * @param initialSize THe initial size of the collection
     * @return A supplier of sets that creates a new set each time it is called
     */
    public <T> Supplier<Set<T>> referenceFactory(ToIntFunction<Object> hasher, Function<? super T, ? extends Reference<T>> referenceFactory, int initialSize) {
        return () -> CollectionUtils.referenceFactorySet(hasher, referenceFactory, initialSize);
    }

    /**
     * Create a weak (or soft, or something else) hash set with full control
     * over the creation and handling of the references used (for example
     * SoftReferences, or WeakReferences that remain strongly referenced until a
     * timeout, or references attached to your own reference queue.
     *
     * @param <T> The type
     * @param referenceFactory The function which creates reference objects
     * @param initialSize THe initial size of the collection
     * @return A supplier of sets that creates a new set each time it is called
     */
    public <T> Supplier<Set<T>> referenceFactory(Function<? super T, ? extends Reference<T>> referenceFactory, int initialSize) {
        return () -> CollectionUtils.referenceFactorySet(IDENTITY_HASH_CODE, referenceFactory, initialSize);
    }

    /**
     * Create a weak (or soft, or something else) hash set with full control
     * over the creation and handling of the references used (for example
     * SoftReferences, or WeakReferences that remain strongly referenced until a
     * timeout, or references attached to your own reference queue.
     *
     * @param <T> The type
     * @param referenceFactory The function which creates reference objects
     * @return A supplier of sets that creates a new set each time it is called
     */
    public <T> Supplier<Set<T>> referenceFactory(Function<? super T, ? extends Reference<T>> referenceFactory) {
        return () -> CollectionUtils.referenceFactorySet(IDENTITY_HASH_CODE, referenceFactory, 20);
    }

    /**
     * Create a Supplier of new sets.
     *
     * @param <T> The type
     * @param initialSize The initial size
     * @param threadSafe If true, wrap any sets produced which are not
     * concurrent set implementations will be wrapped in
     * Collections.synchronizedSet
     * @return A supplier of sets
     */
    @Override
    public <T> Supplier<Set<T>> setSupplier(int initialSize, boolean threadSafe) {
        return () -> set(initialSize, threadSafe);
    }

    /**
     * Create a Set with the characteristics this enum constant applies.
     *
     * @param <T> The type
     * @param initialSize The initial size
     * @param threadSafe If true, wrap any sets produced which are not
     * concurrent set implementations will be wrapped in
     * Collections.synchronizedSet
     * @return A supplier of sets
     */
    public <T> Set<T> set(int targetSize, boolean threadsafe) {
        Set<T> result;
        switch (this) {
            case ORDERED_HASH:
                result = new LinkedHashSet<>(targetSize);
                break;
            case WEAK_HASH:
                result = CollectionUtils.weakSet();
                break;
            case CONCURRENT_EQUALITY:
                // no need to wrap in synchronized set
                return ConcurrentHashMap.newKeySet(targetSize);
            case CONCURRENT_IDENTITY:
                // no need to wrap in synchronized set
                return new ConcurrentSkipListSet<>(SetFactories::hashCodeCompare);
            case UNORDERED_HASH:
                if (threadsafe) {
                    return ConcurrentHashMap.newKeySet(targetSize);
                }
                result = new HashSet<>(targetSize);
                break;
            case IDENTITY:
                // An identity hash set by weird means
                result = new TreeSet<>(SetFactories::hashCodeCompare);
                break;
            default:
                throw new AssertionError(this);
        }
        if (threadsafe) {
            result = Collections.synchronizedSet(result);
        }
        return result;
    }

    static int hashCodeCompare(Object a, Object b) {
        return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
    }
}
