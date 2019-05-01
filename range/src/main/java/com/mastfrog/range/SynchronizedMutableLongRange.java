package com.mastfrog.range;

/**
 *
 * @author Tim Boudreau
 */
final class SynchronizedMutableLongRange extends MutableLongRangeImpl {

    SynchronizedMutableLongRange(long start, long size) {
        super(start, size);
    }

    @Override
    public synchronized boolean setStartAndSize(long start, long size) {
        return super.setStartAndSize(start, size);
    }

    @Override
    public synchronized long size() {
        return super.size();
    }

    @Override
    public synchronized long start() {
        return super.start();
    }

    @Override
    public MutableLongRangeImpl newRange(int start, int size) {
        return new SynchronizedMutableLongRange(start, size);
    }

    @Override
    public MutableLongRangeImpl newRange(long start, long size) {
        return new SynchronizedMutableLongRange((int) start, (int) size);
    }
}
