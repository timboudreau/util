/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.util.collections;

import java.util.Collection;
import java.util.List;
import java.util.function.IntConsumer;

/**
 *
 * @author Tim Boudreau
 */
public interface IntList extends List<Integer> {

    public static IntList create() {
        return new IntListImpl();
    }

    public static IntList create(int initialCapacity) {
        return new IntListImpl(initialCapacity);
    }

    public static IntList create(Collection<? extends Integer> vals) {
        IntListImpl result = new IntListImpl(vals.size());
        result.addAll(vals);
        return result;
    }

    public static IntList createFrom(int... vals) {
        return new IntListImpl(vals);
    }

    void add(int value);

    void add(int index, int element);

    void addAll(int... values);

    void addAll(int index, int... nue);

    void addArray(int... arr);

    boolean contains(int value);

    void forEach(IntConsumer c);

    Integer get(int index);

    int getAsInt(int index);

    int indexOf(int value);

    int lastIndexOf(int i);

    Integer remove(int index);

    void removeAt(int index);

    boolean removeLast();

    int set(int index, int value);

    IntList subList(int fromIndex, int toIndex);

    IntList copy();

    int[] toIntArray();

}
