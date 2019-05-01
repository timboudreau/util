package com.mastfrog.range;

/**
 *
 * @author Tim Boudreau
 */
final class SynchronizedMutableIntRange extends MutableIntRangeImpl {

    SynchronizedMutableIntRange(int start, int size) {
        super(start, size);
    }

    @Override
    public synchronized boolean setStartAndSize(int start, int size) {
        return super.setStartAndSize(start, size);
    }

    @Override
    public synchronized int size() {
        return super.size();
    }

    @Override
    public synchronized int start() {
        return super.start();
    }

    @Override
    public MutableIntRangeImpl newRange(int start, int size) {
        return new SynchronizedMutableIntRange(start, size);
    }

    @Override
    public MutableIntRangeImpl newRange(long start, long size) {
        return new SynchronizedMutableIntRange((int) start, (int) size);
    }
}
