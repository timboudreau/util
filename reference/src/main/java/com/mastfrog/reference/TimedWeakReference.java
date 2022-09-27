package com.mastfrog.reference;

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A weak reference which retains a strong reference to its referent for a
 * settable period after its construction. If a call to <code>get()</code>
 * succeeds <i>after</i> it has expired (i.e. something else still holds a
 * reference to the referent, so it was not garbage collected yet), then the
 * strong reference is restored and the timeout reset.
 * <p>
 * This is a useful pattern, particularly in GUI applications where you have
 * expensive-to-create objects such as a parse tree for a file, that are sure to
 * be needed multiple times immediately following a change to the file, but are
 * typically needed for brief flurries of activity, and should be treated as
 * discardable after some period of time.
 * </p><p>
 * The exact timing of when the reference becomes weak is inexact, but will be
 * no <i>less</i> than the requested delay subsequent to creation or the most
 * recent successful call to <code>get()</code>.
 *
 * @author Tim Boudreau
 */
public abstract class TimedWeakReference<T> extends WeakReference<T> implements TimedReference<T> {

    /**
     * System property to set the time after first use before the first time the
     * timer which removes the strong references from expired
     * TimedWeakReferences will run. In a GUI app like NetBeans (which this was
     * originally developed for), there can be a *lot* of activity during
     * startup, and to avoid thrashing and generating even more work, it is a
     * good idea to have this be a fairly long delay. The default is 90
     * <b>seconds</b>.
     */
    public static final String SYS_PROP_TIMED_WEAK_REFERENCE_TIMER_INITIAL_DELAY_MILLIS
            = "timed.weak.reference.timer.initial.delay";
    /**
     * System property to set how often timed weak references are checked for
     * expiry. The default is 20 seconds.
     */
    public static final String SYS_PROP_TIMED_WEAK_REFERENCE_EXPIRY_CHECK_PERIOD_MILLIS
            = "timed.weak.reference.expiry.check.period";

    /**
     * A TimedWeakReference can have its own custom delay, but it does not make
     * much sense for this to be extremely short. The default is 125
     * milliseconds.
     */
    public static final String SYS_PROP_TIMED_WEAK_REFERENCE_MIN_DELAY_MILLIS
            = "timed.weak.reference.min.delay";

    /**
     * The default delay used by the single-argument constructor.
     */
    public static final int DEFAULT_DELAY_MILLIS
            = parseIntProperty(SYS_PROP_TIMED_WEAK_REFERENCE_EXPIRY_CHECK_PERIOD_MILLIS, 20_000);
    /**
     * The minimum delay allowed to avoid thrashing the queue.
     */
    public static final int MIN_DELAY_MILLIS
            = parseIntProperty(SYS_PROP_TIMED_WEAK_REFERENCE_MIN_DELAY_MILLIS, 125);

    static int parseIntProperty(String name, int defaultValue) {
        String val = System.getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            int newValue = Integer.parseInt(val);
            if (newValue < 0) {
                throw new IllegalArgumentException("Negative value " + newValue);
            }
            return newValue;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid integer value for " + name + ": " + val + ": " + e);
            return defaultValue;
        }
    }

    /**
     * Invoke the passed consumer with the referent if it has not been garbage
     * collected. This method will reset the expiration timer and return this
     * TimedWeakReference to strongly referencing the referent for a new timeout
     * interval, if the referent is available.
     *
     * @param consumer A consumer
     * @return true if the value was non-null and the consumer was invoked
     */
    @Override
    public final boolean ifPresent(Consumer<T> consumer) {
        T obj = get();
        boolean result = obj != null;
        if (result) {
            consumer.accept(obj);
        }
        return result;
    }

    /**
     * Create a new TimedWeakReference with
     * {@link TimedWeakReference#DEFAULT_DELAY_MILLIS} as its timeout to become
     * a weak reference.
     *
     * @param referent The referenced object
     */
    public static <T> TimedWeakReference<T> create(T referent) {
        return new TimedWeakReferenceImpl<>(notNull("referent", referent));
    }

    /**
     * Create a new TimedWeakReference with the passed delay as its timeout to
     * become a weak reference.
     *
     * @param referent The referenced object
     * @param delayMillis The delay in milliseconds
     * @param throws InvalidArgumentException if the delayMillis argument is
     * less than or equal to zero
     */
    public static <T> TimedWeakReference<T> create(T referent, int delayMillis) {
        return new TimedWeakReferenceImpl<>(notNull("referent", referent), delayMillis);
    }

    /**
     * Create a new TimedWeakReference with the passed delay as its timeout to
     * become a weak reference. Delays below {@link MIN_DELAY_MILLIS} will be
     * set to {@link MIN_DELAY_MILLIS}.
     *
     * @param referent The referenced object
     * @param delayUnits The delay in <code>unit</code> units
     * @param unit The time unit of delayUnits
     * @param throws InvalidArgumentException if the argument is less than or
     * equal to zero, or the referent is null
     */
    public static <T> TimedWeakReference<T> create(T referent, int delayUnits,
            TimeUnit unit) {
        return new TimedWeakReferenceImpl<>(notNull("referent", referent),
                delayUnits, unit);
    }

    /**
     * Create a new TimedWeakReference.
     *
     * @param referent The referenced object
     */
    TimedWeakReference(T referent) {
        super(referent, CleanupQueue.queue());
    }

    abstract boolean isStrong(); // for tests

    abstract boolean isExpired(); // for tests

    T rawGet() { // for tests
        return super.get();
    }

    /**
     * Get the referent of this reference - a successful call to
     * <code>get()</code> that returns non-null will restore the strong
     * reference to this timed reference and reset the timer to expire it and
     * destroy that reference, causing the underlying object to live a while
     * longer.
     *
     * @return The referent, if it has not been garbage collected
     */
    @Override
    public T get() {
        // overridden entirely to provide javadoc
        return super.get();
    }
}
