package com.mastfrog.bits.large;

import static com.mastfrog.bits.large.UnsafeUtils.UNSAFE;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tim Boudreau
 */
final class UnsafeLongArray implements CloseableLongArray, OffHeap {

    private long size;
    private long address;
    final Handle handle;

    UnsafeLongArray(long size) {
        this(size, UNSAFE.allocateMemory(Long.BYTES * size));
    }

    UnsafeLongArray(long[] items) {
        this(items.length);
        for (int i = 0; i < items.length; i++) {
            set(i, items[i]);
        }
    }

    UnsafeLongArray(long size, long address) {
        this.size = size;
        this.address = address;
        handle = enqueue();
    }

    UnsafeLongArray(UnsafeLongArray other) {
        this(other.size());
        UNSAFE.copyMemory(other.address, address, size * Long.BYTES);
    }

    public void ensureWriteOrdering(Consumer<LongArray> c) {
        UNSAFE.fullFence();
        try {
            c.accept(this);
        } finally {
            UNSAFE.fullFence();
        }
    }

    @Override
    public boolean isZeroInitialized() {
        return false;
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Object clone() {
        return new UnsafeLongArray(this);
    }

    boolean checkOffset(long pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("Negative position " + pos);
        }
        if (pos > size) {
            throw new IllegalArgumentException("Position "
                    + pos + " is greater than array size " + size);
        }
        return true;
    }

    long address() {
        return address; // for tests
    }

    public void fill(long start, long length, long value) {
        if (length == 0) {
            return;
        } else if (length == 1) {
            assert checkOffset(start);
            assert checkOffset(start + length - 1);
            set(start, value);
        }
        assert checkOffset(start);
        assert checkOffset(start + length - 1);
        if (value == 0) {
            UNSAFE.setMemory(addressOf(start), length * Long.BYTES, (byte) 0);
            return;
        }
        long startAddr = addressOf(start);
        UNSAFE.putLong(startAddr, value);
        // copy one stretch, then duplicate progressively, truncating the
        // last
        long copied = 1;
        while (copied < length) {
            long addr = addressOf(start + copied);
            long toCopy = copied;
            if (copied + toCopy > length) {
                toCopy = length - copied;
            }
            long len = Long.BYTES * toCopy;
            UNSAFE.copyMemory(startAddr, addr, len);
            copied += toCopy;
        }
    }

    @Override
    public void copy(long dest, LongArray from, long start, long length, boolean grow) {
        if (from instanceof UnsafeLongArray) {
            UnsafeLongArray orig = (UnsafeLongArray) from;
            if (dest + length > size()) {
                if (!grow) {
                    length = size() - start;
                } else {
                    resize(dest + length);
                }
            }
            assert checkOffset(dest);
            assert checkOffset(dest + length - 1);
            long origAddress = orig.addressOf(start);
            long destAddress = addressOf(dest);
            UNSAFE.copyMemory(origAddress, destAddress, length * Long.BYTES);
        } else {
            CloseableLongArray.super.copy(dest, from, start, length, grow);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof UnsafeLongArray) {
            UnsafeLongArray un = (UnsafeLongArray) o;
            if (un.address == address && un.size == size) {
                return true;
            }
            if (un.size == size) {
                for (int i = 0; i < size; i++) {
                    if (un.get(i) != get(i)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (long i = 0; i < size; i++) {
            long element = get(i);
            int elementHash = (int) (element ^ (element >>> 32));
            result = 31 * result + elementHash;
        }
        return result;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public void resize(long size) {
        if (size == this.size) {
            return;
        } else if (size < 0) {
            throw new NegativeArraySizeException("Negative size " + size);
        }
        long newSize = size * Long.BYTES;
        address = UNSAFE.reallocateMemory(address, newSize);
        this.size = size;
    }

    private long addressOf(long index) {
        return address + (index * Long.BYTES);
    }

    public void or(long index, long val) {
        if (val == 0) {
            return;
        }
        assert checkOffset(index);
        set(index, get(index) | val);
    }

    @Override
    public void set(long index, long val) {
        assert checkOffset(index);
        long addr = addressOf(index);
        UNSAFE.putLong(addr, val);
    }

    public long get(long index) {
        assert checkOffset(index);
        long addr = addressOf(index);
        return UNSAFE.getLong(addr);
    }

    public void forEach(LongConsumer lc) {
        for (long i = 0; i < size; i++) {
            lc.accept(get(i));
        }
    }

    public void forEachReverse(LongConsumer lc) {
        for (long i = size - 1; i >= 0; i--) {
            lc.accept(get(i));
        }
    }

    public int get(long index, long[] into) {
        assert checkOffset(index);
        int count;
        for (count = 0; count < into.length; count++) {
            if (count > size) {
                break;
            }
            into[count] = get(count + index);
        }
        return count;
    }

    /**
     * Ensures the reference queue will release the memory if this object is
     * garbage collected.
     *
     * @return A reference
     */
    Handle enqueue() {
        startQueueTimer();
        return new Handle(this);
    }

    /**
     * Determine if the backing store for this array has been disposed.
     *
     * @return True if it has been disposed.
     */
    @Override
    public boolean isDisposed() {
        return handle.isDisposed();
    }

    /**
     * Dispose this long array.
     */
    @Override
    public void dispose() {
        handle.dispose();
    }

    /**
     * Dispose this long array.
     */
    @Override
    public void close() {
        dispose();
    }

    @Override
    public void addAll(LongArray longs) {
        if (longs instanceof UnsafeLongArray) {
            UnsafeLongArray ula = (UnsafeLongArray) longs;
            long oldSize = size();
            resize(oldSize + ula.size());
            long addr = addressOf(oldSize);
            UNSAFE.copyMemory(ula.address, addr, longs.size());
        }
        CloseableLongArray.super.addAll(longs);
    }

    static class Handle extends PhantomReference<UnsafeLongArray> {

        private final long address;
        private volatile boolean disposed;

        Handle(UnsafeLongArray referent) {
            super(referent, QUEUE);
            this.address = referent.address;
        }

        boolean isDisposed() {
            return disposed;
        }

        Handle dispose() {
            if (!disposed) {
                disposed = true;
                lastCleared = address;
                UNSAFE.freeMemory(address);
            }
            return this;
        }

        @Override
        public String toString() {
            return get() + " @ " + address;
        }
    }

    static final LongArrayReferenceQueue QUEUE = new LongArrayReferenceQueue();
    private static final long DELAY_MS = 500;
    static final AtomicBoolean TIMER_STARTED = new AtomicBoolean();
    static Timer timer;

    static final void startQueueTimer() {
        if (TIMER_STARTED.compareAndSet(false, true)) {
            timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Thread.currentThread().setName("Off-heap long array cleanup");
                    Handle h;
                    try {
                        while ((h = QUEUE.remove(DELAY_MS - (DELAY_MS / 4))) != null) {
                            // do nothing
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(UnsafeLongArray.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    ;
                }
            }, DELAY_MS, DELAY_MS);
        }
    }

    static long lastCleared = 0;

    static final class LongArrayReferenceQueue extends ReferenceQueue<UnsafeLongArray> {

        @Override
        public Handle remove() throws InterruptedException {
            return cleanup((Handle) super.remove());
        }

        @Override
        public Handle remove(long timeout) throws IllegalArgumentException, InterruptedException {
            return cleanup((Handle) super.remove(timeout));
        }

        @Override
        public Handle poll() {
            return cleanup((Handle) super.poll());
        }

        Handle cleanup(Handle h) {
            if (h != null) {
                return h.dispose();
            }
            return h;
        }
    }

//
//    public static long bytesToLong(byte[] b) {
//        long result = 0;
//        for (int i = 0; i < 8; i++) {
//            result <<= 8;
//            result |= (b[i] & 0xFF);
//        }
//        return result;
//    }
}
