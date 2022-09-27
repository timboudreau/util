/* 
 * The MIT License
 *
 * Copyright 2020 Tim Boudreau.
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
package com.mastfrog.reference;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic object pool where objects are associated with an owner and returned
 * to a collection of available items when the owner is garbage collected. Users
 * must either call start() to launch a background thread to recycle references,
 * or the user must call <code>reclaimationPoll()</code> or
 * <code>reclaimAll()</code> periodically, or it is possible to run out of
 * memory by piling up unexpired phantom references. The takeFromPool method
 * will poll and reuse an instance if one is available, but this is not
 * guaranteed, since it depends on the behavior of the garbage collector.
 * <p>
 * Note: When associating pooled objects with the lifecycle of another object,
 * it is important to use the object with the shortest lifecycle practical - the
 * pooled object will remain referenced and unreclaimed for <i>as long as the
 * object it was associated with survives</i> whether or not it is still in use.
 * </p>
 *
 * @author Tim Boudreau
 */
final class PhantomReferencePool<T> implements ReferencePool<T> {

    // Pending - Phant and DebugPhant can likely implement Runnable and use CleanupQueue
    // directly instead of a dedicated thread - this was consolidated here from
    // a different project.
    private static final boolean DEBUG
            = Boolean.getBoolean(SYS_PROP_PHANTOM_REFERENCE_POOL_DEBUG);
    private final int maxSize;
    private final ReferenceQueue<Object> rq = new ReferenceQueue<>();
    private final ObjectBag<T> available = ObjectBag.create();
    private final AtomicInteger availableSize = new AtomicInteger();
    private final Supplier<T> newSupplier;
    private final Thread t;
    private final Set<Phant<T>> phantoms;
    private final String poolName;
    private volatile int recycled;
    private volatile boolean stopped;

    PhantomReferencePool(String poolName, int maxSize, Supplier<T> newSupplier) {
        this(Thread.NORM_PRIORITY - 1, poolName, maxSize, newSupplier);
    }

    PhantomReferencePool(int reclamationThreadPriority, String poolName, int maxSize,
            Supplier<T> newSupplier) {
        this.newSupplier = newSupplier;
        this.maxSize = maxSize;
        phantoms = ConcurrentHashMap.newKeySet(maxSize);
        this.poolName = poolName;
        t = new Thread(this::reclamationLoop);
        t.setName(poolName + " reclamation thread");
        t.setDaemon(true);
        t.setPriority(reclamationThreadPriority);
    }

    public int maximumSize() {
        return maxSize;
    }

    public int recycled() {
        return recycled;
    }

    @Override
    public String toString() {
        return poolName + " pool with available " + available()
                + " outstanding " + outstanding()
                + " actual avail size " + available.size()
                + " recycled " + recycled + " instances over lifetime";
    }

    public int available() {
        return availableSize.get();
    }

    public int outstanding() {
        return phantoms.size();
    }

    void stop() {
        stopped = true;
        if (t.isAlive()) {
            t.interrupt();
        }
        phantoms.clear();
        available.clear();
        availableSize.set(0);
    }

    public boolean isReclamationRunning() {
        return t.isAlive();
    }

    public void start() {
        if (!t.isAlive()) {
            t.start();
        }
    }

    void reclamationLoop() {
        for (;;) {
            try {
                reclaimationInner();
            } catch (InterruptedException ex) {
                if (!stopped) {
                    Logger.getLogger(PhantomReferencePool.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (Throwable ex) {
                Logger.getLogger(PhantomReferencePool.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (stopped) {
                    return;
                }
            }
        }
    }

    public void reclaimAvailable() {
        T obj;
        do {
            obj = reclaimationPoll();
        } while (obj != null);
    }

    @SuppressWarnings("unchecked")
    public T reclaimationPoll() {
        Phant<T> ref = (Phant<T>) rq.poll();
        if (ref != null) {
            phantoms.remove(ref);
            T toRecycle = ref.remove();
            if (toRecycle != null) {
                recycled++;
                reclaim(toRecycle);
            }
            return toRecycle;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private T reclaimationInner() throws InterruptedException {
        Phant<T> ref = (Phant<T>) rq.remove();
        if (ref != null) {
            phantoms.remove(ref);
            T toRecycle = ref.remove();
            if (toRecycle != null) {
                recycled++;
                reclaim(toRecycle);
            }
            return toRecycle;
        }
        return null;
    }

    /**
     * Borrow an object from the pool, passing it to the consumer and returning
     * it to the pool as soon as the consumer has completed. The consumer
     * <i>must not</i> retain a reference to the pooled object or pass it to
     * other code that might.
     *
     * @param consumer A consumer
     */
    @Override
    public void borrow(Consumer<T> consumer) {
        T obj = takeFromPool();
        try {
            consumer.accept(obj);
        } finally {
            returnToPool(obj);
        }
    }

    /**
     * Return an object to the pool which was take using the no-argument
     * <code>takeFromPool()</code> method.
     *
     * @throws AssertionError if the object passed here is already associated
     * with some object's lifecycle, as that could cause a pooled item to be in
     * use by more than one object concurrently.
     * @param obj The object to return to the pool
     */
    @Override
    public void returnToPool(T obj) {
        reclaim(obj);
        assert !isOwned(obj) :
                "Returning an object owned "
                + "by a reference";
    }

    private boolean isOwned(T obj) {
        for (Phant<T> p : phantoms) {
            if (p.poolItem.get() == obj) {
                if (p instanceof DebugPhant<?>) {
                    throw new AssertionError("Returning an object owned by "
                            + " instance " + ((DebugPhant<?>) p).referentIdentity
                            + " of type " + (((DebugPhant<?>) p)).referentClass);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Take an object from the pool for use by an object which has not yet been
     * constructed; the BiFunction will be called back with the pooled object
     * and a consumer to accept the owning object once it is contructed, and
     * return whatever the function returns (typically the constructed object).
     * <p/>
     * Note that failing to call the passed consumer permanently removes it from
     * the pool.
     *
     * @param <R> The type of object the function will return
     * @param c A BiFunction which will be passed the pooled object and a
     * consumer to associate it with the lifecycle of the object passed to it.
     * @return The return value of the passed function.
     */
    @Override
    public <R> R lazyTakeFromPool(BiFunction<T, Consumer<Object>, R> c) {
        T obj = takeFromPool(null);
        return c.apply(obj, owner -> {
            Phant<T> p = newReference(obj, owner, rq);
            phantoms.add(p);
        });
    }

    /**
     * Take an object from the pool, <b>not associating it with any object's
     * lifecycle</b> because you will return it by explicitly calling
     * returnToPool with it when done with it. This allows quick temporary use
     * of an object from the pool inside a try/finally block. Failing to call
     * returnToPool() with the object returned here means that object is
     * permanently lost to the pool.
     *
     * @return The object
     */
    @Override
    public T takeFromPool() {
        return takeFromPool(null);
    }

    private long lastLog = System.currentTimeMillis();

    /**
     * Take an item from the pool, associating it with the passed object, and
     * returning it to the pool only if and when that object is garbage
     * collected.
     *
     * @param owner The owner object
     * @return An item from the pool if one is available, or a newly created one
     * if not
     */
    @SuppressWarnings("unchecked")
    @Override
    public T takeFromPool(Object owner) {
        T item = null;
        Phant<T> p = (Phant<T>) rq.poll();
        if (p != null) {
            item = p.remove();
            recycled++;
            phantoms.remove(p);
        }
        if (item == null) {
            item = available.removeOne();
            if (item == null) {
                item = newSupplier.get();
                availableSize.lazySet(0);
            } else {
                decrementCount();
            }
        }
        if (owner != null) {
            p = newReference(item, owner, rq);
            phantoms.add(p);
        }
        maybeLog();
        return item;
    }

    private void maybeLog() {
        if (System.currentTimeMillis() - lastLog > 20_000) {
            System.out.println(this);
            lastLog = System.currentTimeMillis();
            if (DEBUG && outstanding() > maximumSize()) {
                logPiggies();
            }
        }
    }

    private void logPiggies() {
        Map<String, Set<Integer>> byType = new TreeMap<>();
        Map<String, Integer> byCount = new TreeMap<>();
        Map<Integer, Integer> byInstance = new TreeMap<>();
        int max = 0;
        int maxPer = 0;
        for (Phant<T> p : phantoms) {
            if (!p.isRecycled() && p instanceof DebugPhant<?>) {
                DebugPhant<?> debug = (DebugPhant<?>) p;
                byType.computeIfAbsent(debug.referentClass, rc -> new TreeSet<>()).add(debug.referentIdentity);
                int ct = byCount.get(debug.referentClass) + 1;
                byCount.put(debug.referentClass, ct);
                max = Math.max(ct, max);
                int byIdCt = byInstance.get(debug.referentIdentity) + 1;
                byInstance.put(debug.referentIdentity, byIdCt);
                maxPer = Math.max(byIdCt, maxPer);
            }
        }
        if (maxPer > 3 || max > 5) {
            StringBuilder sb = new StringBuilder("Piggies:");
            for (Map.Entry<String, Set<Integer>> e : byType.entrySet()) {
                sb.append("\n  ").append(e.getKey()).append(e.getValue().size()).append(" instances:");
                for (Integer i : e.getValue()) {
                    sb.append("\n    ").append(i).append(": ").append(byInstance.get(i));
                }
            }
            System.out.println(sb);
        }
    }

    void reclaim(T obj) {
        int ct = incrementCount();
        if (ct < maxSize) {
            available.add(obj);
        }
    }

    private int incrementCount() {
        return availableSize.getAndUpdate(curr -> {
            return Math.min(maxSize, curr + 1);
        });
    }

    private int decrementCount() {
        return availableSize.getAndUpdate(curr -> {
            return Math.max(0, curr - 1);
        });
    }

    @SuppressWarnings("unchecked")
    private Phant<T> newReference(T poolItem, Object o, ReferenceQueue q) {
        if (DEBUG) {
            return new DebugPhant<>(poolItem, o, q);
        } else {
            return new Phant<>(poolItem, o, q);
        }
    }

    private static class Phant<T> extends PhantomReference<Object> {

        private AtomicReference<T> poolItem;

        Phant(T poolItem, Object referent, ReferenceQueue<? super Object> q) {
            super(referent, q);
            this.poolItem = new AtomicReference<>(poolItem);
        }

        T remove() {
            clear();
            return poolItem.getAndSet(null);
        }

        @Override
        public void clear() {
            super.clear();
        }

        boolean isRecycled() {
            return poolItem.get() == null;
        }
    }

    private final static class DebugPhant<T> extends Phant<T> {

        final String referentClass;
        final int referentIdentity;

        DebugPhant(T poolItem, Object referent, ReferenceQueue<? super Object> q) {
            super(poolItem, referent, q);
            referentClass = referent.getClass().getName();
            referentIdentity = System.identityHashCode(referent);
        }
    }
}
