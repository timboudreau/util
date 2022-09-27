/*
 * Copyright 2020 Mastfrog Technologies.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastfrog.reference;

import com.mastfrog.util.preconditions.Checks;
import static com.mastfrog.util.preconditions.Checks.greaterThanZero;
import static com.mastfrog.util.preconditions.Checks.notNull;
import com.mastfrog.util.preconditions.InvalidArgumentException;
import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.newSetFromMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A weak reference which only becomes weak some time after a
 * post-creation/post-last-use delay expires; calls to <code>get()</code> reset
 * the delay. Note that the timing of when exactly the reference becomes weak is
 * inexact, but will be no <i>less</i> than the requested delay.
 *
 * @author Tim Boudreau
 */
final class TimedWeakReferenceImpl<T> extends TimedWeakReference<T> implements Runnable {

    static final int INITIAL_DELAY = parseIntProperty(
            SYS_PROP_TIMED_WEAK_REFERENCE_TIMER_INITIAL_DELAY_MILLIS, 90_000);

    private static final Timer timer = new Timer("timed-weak-refs", true);
    static final CleanupTask TASK = new CleanupTask();

    private static volatile boolean SHUTTING_DOWN;
    // Use an atomic, lockless, linked data structure to manage instances
    // that are waiting to have their strong references removed
    private static final ObjectBag<TimedWeakReferenceImpl<?>> INSTANCES
            = ObjectBag.create();
    /**
     * The time at which this instance becomes expired.
     */
    private volatile long expiryTimeMillis;
    /**
     * The expiry delay.
     */
    private final int delayMillis;
    /**
     * The initial strong reference; note this need not be volatile - the only
     * thing that could be affected by an out-of-order read on it is the garbage
     * collector, and at worst reclamation is delayed by one GC cycle.
     */
    private T strong;

    /**
     * Create a new TimedWeakReference with
     * {@link DEFAULT_DELAY#DEFAULT_DELAY_MILLIS} as its timeout to become a
     * weak reference.
     *
     * @param referent The referenced object
     */
    @SuppressWarnings("LeakingThisInConstructor")
    TimedWeakReferenceImpl(T referent) {
        super(referent);
        strong = referent;
        expiryTimeMillis = System.currentTimeMillis()
                + DEFAULT_DELAY_MILLIS;
        this.delayMillis = DEFAULT_DELAY_MILLIS;
        enqueue(this);
    }

    /**
     * Create a new TimedWeakReference with the passed delay as its timeout to
     * become a weak reference.
     *
     * @param referent The referenced object
     * @param delay The delay in <code>unit</code> units
     * @param unit The time unit
     */
    TimedWeakReferenceImpl(T referent, int delay, TimeUnit unit)
            throws InvalidArgumentException {
        this(referent, (int) unit.toMillis(Checks.greaterThanZero("delay", delay)));
    }

    /**
     * Create a new TimedWeakReference with the passed delay as its timeout to
     * become a weak reference. Delays below {@link MIN_DELAY_MILLIS} will be
     * set to {@link MIN_DELAY_MILLIS}.
     *
     * @param referent The referenced object
     * @param delayMillis The delay in milliseconds
     * @param throws InvalidArgumentException if the argument is less than or
     * equal to zero
     */
    @SuppressWarnings("LeakingThisInConstructor")
    TimedWeakReferenceImpl(T referent, int delayMillis) throws InvalidArgumentException {
        super(notNull("referent", referent));
        strong = referent;
        expiryTimeMillis = currentTimeMillis()
                + Math.max(MIN_DELAY_MILLIS,
                        greaterThanZero("delay", delayMillis));
        this.delayMillis = delayMillis;
        enqueue(this);
    }

    @Override
    public String toString() {
        long remaining = max(0L, expiryTimeMillis - currentTimeMillis());
        return "W(" + delayMillis + " " + remaining + " remain " + stringValue()
                + " exp " + (remaining == 0L) + ")";
    }

    private String stringValue() {
        T obj = strong;
        if (obj == null) {
            obj = rawGet();
        }
        if (obj == null) {
            return "null";
        }
        return Objects.toString(obj);
    }

    /**
     * Get the object if it is non-null and passes the test of the pssed
     * predicate; does not update the expiration delay or return this reference
     * to strong status unless the predicate returns true, so objects which are
     * obsolete for some reason other than age do not get garbage collected even
     * later due to testing for obsolescence.
     *
     * @param pred A predicate
     *
     * @return The referenced object, if it still exists and if it passes the
     * predicate's test
     */
    @Override
    public T getIf(Predicate<T> pred) {
        T obj = strong;
        if (obj == null) {
            obj = super.get();
        }
        if (obj != null) {
            if (pred.test(obj)) {
                strong = obj;
                touch();
                return obj;
            }
        }
        return null;
    }

    void touch() {
        expiryTimeMillis = System.currentTimeMillis() + delayMillis;
    }

    @Override
    boolean isExpired() {
        return System.currentTimeMillis() > expiryTimeMillis;
    }

    /**
     * Force this object to weak reference status and expire it; note that a
     * subsequent call to <code>get()</code> can revive it.
     */
    @Override
    public void discard() {
        strong = null;
        expiryTimeMillis = 0;
        INSTANCES.remove(this);
    }

    @Override
    public T get() {
        T st = strong;
        boolean wasWeak;
        if (st != null) {
            touch();
            // another thread may have expired us while we're in here
            strong = st;
            maybeEnqueue(this);
            return st;
        } else {
            wasWeak = true;
        }
        T result = super.get();
        if (result != null) {
            touch();
            strong = result;
            if (wasWeak) {
                maybeEnqueue(this);
            }
        }
        return result;
    }

    @Override
    T rawGet() {
        return super.get();
    }

    void reallyBecomeWeak() {
        strong = null;
    }

    @Override
    boolean isStrong() {
        return strong != null;
    }

    static {
        timer.scheduleAtFixedRate(TASK, INITIAL_DELAY,
                DEFAULT_DELAY_MILLIS + (DEFAULT_DELAY_MILLIS / 2));
        Thread t = new Thread(() -> {
            SHUTTING_DOWN = true;
            TASK.run();
        }, "shutdown-timed-ref-cleanup");
        Runtime.getRuntime().addShutdownHook(t);
    }

    /**
     * Runs when the reference has been reclaimed by the garbage collector.
     */
    @Override
    public void run() {
        // ensure we're removed - the referent is gone
        INSTANCES.remove(this);
    }

    private static void maybeEnqueue(TimedWeakReferenceImpl ref) {
        while (!INSTANCES.contains(ref) && !SHUTTING_DOWN && ref.isStrong()) {
            enqueue(ref);
        }
    }

    private static void enqueue(TimedWeakReferenceImpl ref) {
        INSTANCES.add(ref);
    }

    static Set<TimedWeakReferenceImpl<?>> collectExpiredReferences() {
        Set<TimedWeakReferenceImpl<?>> result = newSetFromMap(new IdentityHashMap<>());
        INSTANCES.removing(item -> {
            boolean exp = item.isExpired();
            if (exp) {
                result.add(item);
            }
            return exp;
        });
        return result;
    }

    static void makeExpiredReferencesWeak() {
        Set<TimedWeakReferenceImpl<?>> items = collectExpiredReferences();
        try {
            for (Iterator<TimedWeakReferenceImpl<?>> it = items.iterator(); it.hasNext();) {
                TimedWeakReferenceImpl ti = it.next();
                // If it was touched while we were collecting instances, it may
                // have gone back to strong status, and we should add it back into
                // instances
                if (ti.isExpired()) {
                    ti.reallyBecomeWeak();
                    it.remove();
                }
            }
            INSTANCES.adding(items);
        } catch (Exception | Error e) {
            Logger.getLogger(TimedWeakReferenceImpl.class.getName()).log(
                    Level.SEVERE, "Exception removing " + items, e);
        }
    }

    static final class CleanupTask extends TimerTask {

        @Override
        public void run() {
            if (SHUTTING_DOWN) {
                while (!INSTANCES.isEmpty()) {
                    INSTANCES.drain().forEach(item -> {
                        item.discard();
                    });
                }
                timer.cancel();
            }
            makeExpiredReferencesWeak();
        }
    }
}
