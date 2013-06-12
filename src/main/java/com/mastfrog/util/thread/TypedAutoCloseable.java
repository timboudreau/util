package com.mastfrog.util.thread;

/**
 * A subtype of AutoCloseable parameterized on the exception type it throws
 *
 * @author Tim Boudreau
 * @param <E> The type of exception the close method throws
 */
public abstract class TypedAutoCloseable<E extends Exception> implements AutoCloseable {

    @Override
    public abstract void close() throws E;
}
