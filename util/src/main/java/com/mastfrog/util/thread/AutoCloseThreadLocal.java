package com.mastfrog.util.thread;

/**
 * A wrapper for a ThreadLocal which allows use of try-with-resources to set the
 * value and ensure it gets cleared.
 *
 * @author Tim Boudreau
 */
public class AutoCloseThreadLocal<T> {

    private final ThreadLocal<T> threadLocal = new ThreadLocal<>();

    /**
     * Set the value
     *
     * @param obj The value
     * @return an AutoClosable whose close() method will reset the value to its
     * previous state, or clear it if it was previously unset
     */
    public QuietAutoCloseable set(T obj) {
        final T old = threadLocal.get();
        threadLocal.set(obj);
        return new QuietAutoCloseable() {

            @Override
            public void close() {
                if (old != null) {
                    threadLocal.set(old);
                } else {
                    threadLocal.remove();
                }
            }
        };
    }

    /**
     * Clear the value.  Prever using the AutoCloseable to clear where possible.
     * @return this
     */
    public AutoCloseThreadLocal clear() {
        threadLocal.remove();
        return this;
    }

    /**
     * Get the value
     * @return The value
     */
    public T get() {
        return threadLocal.get();
    }
}
