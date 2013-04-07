/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
package com.mastfrog.util.collections;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Handles a few collections omissions
 *
 * @author Tim Boudreau
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * Create a list which wrappers another list and converts the contents on
     * demand
     *
     * @param <T> The old type
     * @param <R> The new type
     * @param list A list
     * @param converter A thing which converts between types
     * @param fromType The old type
     * @param toType The new type
     * @return A list
     */
    public static <T, R> List<R> convertedList(List<T> list, Converter<R, T> converter, Class<T> fromType, Class<R> toType) {
        return new ConvertList<>(toType, fromType, list, converter);
    }

    /**
     * Create an iterator which converts objects from one iterator into another
     * kind of object on the fly.
     *
     * @param c A thing that converts objects
     * @param iter An iterator
     * @return An iterator
     */
    public static <T, R> ListIterator<R> convertedIterator(Converter<R, T> c, Iterator<T> iter) {
        return convertedIterator(new WrapAsListIterator<>(iter), c);
    }

    /**
     * Covert a list iterator of one object type to another type of object
     *
     * @param iter An iterator
     * @param c The thing that converts objects
     * @return A list iterator
     */
    public static <T, R> ListIterator<R> convertedIterator(ListIterator<T> iter, Converter<R, T> c) {
        return new ConvertIterator<>(iter, c);
    }

    /**
     * Creates a list which uses object identity, not equals() to determine if
     * an object is a member or not, so methods such as remove() will only match
     * the same object even if two objects which equals() the object to remove
     * are available.
     * <p/>
     * The resulting List's own equals() and hashCode() methods are also
     * identity checks.
     */
    public static <T> List<T> identityList(Collection<T> collection) {
        return new IdentityList<>(collection);
    }

    /**
     * Create a List which uses identity comparisons to determine membership
     *
     * @param <T> A type
     * @return A lisst
     */
    public static <T> List<T> newIdentityList() {
        return new IdentityList<>();
    }

    /**
     * Invert a converter
     *
     * @param c A converter
     * @return A converter
     */
    public static <T, R> Converter<T, R> reverseConverter(Converter<R, T> c) {
        return new ReverseConverter(c);
    }

    /**
     * Wrap an iterator in JDK 6's Iterable
     *
     * @param iterator An iterator
     * @return An Iterable
     */
    public static <T> Iterable<T> toIterable(final Iterator<T> iterator) {
        return new IteratorIterable<>(iterator);
    }

    private static class IteratorIterable<T> implements Iterable<T> {

        private final Iterator<T> iter;

        public IteratorIterable(Iterator<T> iter) {
            this.iter = iter;
        }

        @Override
        public Iterator<T> iterator() {
            return iter;
        }
    }

    public static <T> Iterable<T> toIterable(final Enumeration<T> enumeration) {
        return new EnumIterable<>(enumeration);
    }

    private static final class EnumIterable<T> implements Iterable<T> {

        private final Enumeration<T> en;

        public EnumIterable(Enumeration<T> en) {
            this.en = en;
        }

        @Override
        public Iterator<T> iterator() {
            return toIterator(en);
        }
    }

    public static <T> Iterator<T> toIterator(final Enumeration<T> enumeration) {
        return new EnumIterator<>(enumeration);
    }

    private static final class EnumIterator<T> implements Iterator<T>, Iterable<T> {

        private final Enumeration<T> enumeration;

        public EnumIterator(Enumeration<T> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        public boolean hasNext() {
            return enumeration.hasMoreElements();
        }

        @Override
        public T next() {
            return enumeration.nextElement();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }
    }
}
