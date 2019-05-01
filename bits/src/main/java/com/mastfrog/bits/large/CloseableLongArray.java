package com.mastfrog.bits.large;

/**
 *
 * @author Tim Boudreau
 */
public interface CloseableLongArray extends LongArray, AutoCloseable {

    void close();
}
