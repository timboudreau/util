/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.util.collections;

import com.mastfrog.util.search.Bias;
import com.mastfrog.util.search.BinarySearch;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

/**
 * Optimized primitive-int-array based list.
 *
 * @author Tim Boudreau
 */
public interface IntList extends List<Integer> {

    static IntList create() {
        return new IntListImpl();
    }

    static IntList create(int initialCapacity) {
        return new IntListImpl(initialCapacity);
    }

    static IntList create(Collection<? extends Integer> vals) {
        IntListImpl result = new IntListImpl(vals.size());
        result.addAll(vals);
        return result;
    }

    static IntList createFrom(int... vals) {
        return new IntListImpl(vals);
    }

    /**
     * Add a primitive int.
     *
     * @param value The value
     */
    void add(int value);

    /**
     * Add a primitive int at the specified index. Note that this involves array
     * copies and the need to call this method suggests that this class is the
     * wrong tool for the job.
     *
     * @param index An index
     * @param element An element
     */
    void add(int index, int element);

    /**
     * Append an array of primitive ints to this list.
     *
     * @param values
     */
    void addAll(int... values);

    /**
     * Insert an array of primitive ints into this list at the specified
     * position. Note that this involves array copies and the need to call this
     * method suggests that this class is the wrong tool for the job.
     *
     * @param index
     * @param nue
     */
    void addAll(int index, int... nue);

    /**
     * Append an array of primitive ints to this list.
     *
     * @param values
     */
    void addArray(int... arr);

    /**
     * Determine if a primitive int is contained by this list.
     *
     * @param value A value
     * @return true if it is present
     */
    boolean contains(int value);

    /**
     * Iterate with an IntConsumer.
     *
     * @param c A consumer
     */
    void forEach(IntConsumer c);

    /**
     * Implements List.get()
     *
     * @param index The index
     * @return
     */
    Integer get(int index);

    /**
     * Primitive int getter.
     *
     * @param index
     * @return
     */
    int getAsInt(int index);

    /**
     * Get the index of the requested value.
     *
     * @param value A value
     * @return an index or -1
     */
    int indexOf(int value);

    /**
     * Get the last index of a value.
     *
     * @param i An value
     * @return The index or -1
     */
    int lastIndexOf(int i);

    /**
     * Implements List.remove()
     *
     * @param index An index
     * @return An integer if one was present
     */
    Integer remove(int index);

    /**
     * Remove an element at the specified index.
     *
     * @param index The index
     */
    void removeAt(int index);

    /**
     * Remove the last element.
     *
     * @return true if there was an element to remove
     */
    boolean removeLast();

    /**
     * Set the value at some index to a primitive int value.
     *
     * @param index The index, greater than or equal to 0 and less than size
     * @param value The value
     * @return The old value at that position
     */
    int set(int index, int value);

    /**
     * Create a sublist - overloads List.subList to return IntList.
     *
     * @param fromIndex An index
     * @param toIndex Another index >= the first
     * @return A list
     */
    IntList subList(int fromIndex, int toIndex);

    /**
     * Create a copy of this list whose contents are unaffected by changes to
     * this one and vice-versa.
     *
     * @return A copy of this list
     */
    IntList copy();

    /**
     * Create an int array from this list's contents.
     *
     * @return An array
     */
    int[] toIntArray();

    /**
     * Get the first element.
     *
     * @return The first int
     * @throws NoSuchElementException if empty
     */
    int first();

    /**
     * Get the last element.
     *
     * @return The last int
     * @throws NoSuchElementException if empty
     */
    int last();

    /**
     * Get the index of a value, using binary search.
     * <i>If this list is not actually sorted, this method may do anything at
     * all, including go into an endless loop.</i>
     *
     * @return The index of the exact value passed
     * @throws NoSuchElementException if empty
     */
    int indexOfPresumingSorted(int value);

    /**
     * Get the index of a value, using binary search, or the index of a value
     * closest to the passed one, according to the passed Bias argument.
     * <i>If this list is not actually sorted, this method may do anything at
     * all, including go into an endless loop.</i>
     * <p>
     * The Bias parameter works as follows: Say we have a list of
     * <code>0, 10, 20, 30</code>, and you ask for the nearest index to 13. The
     * results will be as follows:
     * </p>
     * <ul>
     * <li>Bias.NONE - there is no exact value of 13 in the list, so you get
     * -1</li>
     * <li>Bias.FORWARD - returns 2, since the nearest value greater than 13 is
     * the 20 at index 2</li>
     * <li>Bias.BACKWARD - returns 1, since the nearest value less than 13 is
     * the 10 at index 1</li>
     * <li>Bias.NEAREST - returns 1, since Math.abs(20 - 13) is greater than
     * Math.abs(10 - 13), so 10 is closer</li>
     * </ul>
     * <p>
     * In the event of ambiguity (equidistant possible answers and requesting
     * Bias.NEAREST), Bias.FORWARD is preferred.
     * </p>
     *
     * @see BinarySearch
     * @see Bias
     * @return The index of the exact value passed
     * @throws NoSuchElementException if empty
     */
    int nearestIndexToPresumingSorted(int value, Bias bias);

    /**
     * Sort this list, altering its internal contents.
     */
    void sort();

    /**
     * Overloads List.Iterator to return PrimitiveIterator.OfInt.
     *
     * @return An iterator
     */
    PrimitiveIterator.OfInt iterator();
}
