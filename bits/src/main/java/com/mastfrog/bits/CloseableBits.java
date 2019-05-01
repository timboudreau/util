package com.mastfrog.bits;

/**
 * Extension to MutableBits for those implementations backed by
 * files or off-heap memory, to explicitly delete the file or reclaim
 * the memory.
 *
 * @author Tim Boudreau
 */
public interface CloseableBits extends MutableBits, AutoCloseable {

    @Override
    public void close();
}
