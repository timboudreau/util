/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
package com.mastfrog.function.throwing;

import com.mastfrog.function.state.Obj;
import static com.mastfrog.util.preconditions.Checks.notNull;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A runnable-alike that can throw Exceptions.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingRunnable {

    public static final ThrowingRunnable NO_OP = new NoOpThrowingRunnable();

    /**
     * Returns a runnable which can be added to asynchronously (all andThen()
     * style methods return <code>this</code>) and which will discard all
     * composed-in runnables once run is called.
     *
     * @return A ThrowingRunnable with the described characteristics
     */
    public static ThrowingRunnable oneShot() {
        return new ComposableAtomicThrowingRunnable(true, false);
    }

    /**
     * Returns a runnable which can be added to asynchronously (all andThen()
     * style methods return the instance).
     *
     * @return A ThrowingRunnable with the described characteristics
     */
    public static ThrowingRunnable composable() {
        return new ComposableAtomicThrowingRunnable(false, false);
    }

    /**
     * Returns a runnable which can be added to asynchronously (all andThen()
     * style methods return the instance) and which will discard all composed-in
     * runnables once run is called.
     *
     * @param lifo If true, use last-in / first-out order for running (reverses
     * the distinction between <code>andAlwaysRun()</code> and
     * <code>andAlwaysRunFirst()</code>)
     * @return A ThrowingRunnable with the described characteristics
     */
    public static ThrowingRunnable oneShot(boolean lifo) {
        return new ComposableAtomicThrowingRunnable(true, lifo);
    }

    /**
     * Returns a runnable which can be added to asynchronously (all andThen()
     * style methods return the instance).
     *
     * @param lifo If true, use last-in / first-out order for running (reverses
     * the distinction between <code>andAlwaysRun()</code> and
     * <code>andAlwaysRunFirst()</code>)
     * @return A ThrowingRunnable with the described characteristics
     */
    public static ThrowingRunnable composable(boolean lifo) {
        return new ComposableAtomicThrowingRunnable(false, lifo);
    }

    /**
     * Wrap a ThrowingRunnable in another one which weakly references the
     * original.
     *
     * @param delegate A throwing runnable
     * @return A wrapper ThrowingRunnable that does not hold a strong reference
     * to the argument passed
     * @throws NullArgumentException if the delegate is null
     */
    public static ThrowingRunnable weak(ThrowingRunnable delegate) {
        if (delegate instanceof ThrowingRunnableWeakDelegating) {
            return delegate;
        }
        return new ThrowingRunnableWeakDelegating(notNull("delegate", delegate));
    }

    /**
     * Do whatever work this object does.
     *
     * @throws Exception If something goes wrong
     */
    void run() throws Exception;

    /**
     * Creates a Java Runnable which may throw undeclared exceptions.
     *
     * @return A runnable
     */
    default Runnable toRunnable() {
        return () -> {
            try {
                ThrowingRunnable.this.run();
            } catch (Exception e) {
                Exceptions.chuck(e);
            }
        };
    }

    default ThrowingRunnable andThen(ThrowingRunnable run) {
        return () -> {
            ThrowingRunnable.this.run();
            run.run();
        };
    }

    /**
     * Variant on <code>andThen</code> which guarantees that the passed runnable
     * will be run <i>regardless of whether this one's run() method throws an
     * exception</i> - if both throw exceptions, the second's exception will be
     * added as a suppressed exception to the first.
     *
     * @param run Another runnable
     * @return a wrapper around this and the other
     */
    default ThrowingRunnable andAlways(ThrowingRunnable run) {
        return () -> {
            Obj<Throwable> ex = Obj.create();
            try {
                ThrowingRunnable.this.run();
            } catch (Exception | Error ex1) {
                ex.set(ex1);
            } finally {
                try {
                    run.run();
                } catch (Exception | Error ex2) {
                    ex.apply(old -> {
                        if (old != null) {
                            old.addSuppressed(ex2);
                            return old;
                        }
                        return ex2;
                    });
                }
            }
            // Rethrow if present
            ex.ifNotNull(Exceptions::chuck);
        };
    }

    /**
     * Variant on <code>andThen</code> which guarantees that the passed runnable
     * will be run <i>regardless of whether this one's run() method throws an
     * exception</i>, and which guarantees that the passed runable is run
     * <b>before</b>
     * this one's <code>run()</code> method - if both throw exceptions, the
     * first's exception will be added as a suppressed exception to the
     * second's.
     *
     * @param run Another runnable
     * @return a wrapper around this and the other
     */
    default ThrowingRunnable andAlwaysFirst(ThrowingRunnable run) {
        return () -> {
            Obj<Throwable> ex = Obj.create();
            try {
                run.run();
            } catch (Exception ex1) {
                ex.set(ex1);
            } finally {
                try {
                    ThrowingRunnable.this.run();
                } catch (Exception | Error ex2) {
                    ex.apply(old -> {
                        if (old != null) {
                            old.addSuppressed(ex2);
                            return old;
                        }
                        return ex2;
                    });
                }
            }
            // Rethrow if present
            ex.ifNotNull(Exceptions::chuck);
        };
    }

    /**
     * Create a ThrowingRunnable which invokes the passed supplier and passes
     * its output to the passed consumer whenever <code>run()</code> is called.
     *
     * @param <T>
     * @param supp
     * @param cons
     * @return
     */
    default <T> ThrowingRunnable of(ThrowingSupplier<T> supp, ThrowingConsumer<T> cons) {
        return () -> {
            cons.accept(supp.get());
        };
    }

    default ThrowingRunnable of(Runnable r) {
        return r::run;
    }

    /**
     * Variant on <code>andThen</code> which guarantees that the passed runnable
     * will be run <i>regardless of whether this one's run() method throws an
     * exception</i> - if both throw exceptions, the second's exception will be
     * added as a suppressed exception to the first. Offers the same
     * functionality as <code>andAlways(ThrowingRunnable)</code> for
     * <code>Runnable</code>s - named differently so lambda users will not have
     * to cast as one or the other.
     *
     * @param run Another runnable
     * @return a wrapper around this and the other
     */
    default ThrowingRunnable andAlwaysRun(Runnable run) {
        return () -> {
            Obj<Throwable> ex = Obj.create();
            try {
                ThrowingRunnable.this.run();
            } catch (Exception ex1) {
                ex.set(ex1);
            } finally {
                try {
                    run.run();
                } catch (Exception | Error ex2) {
                    ex.apply(old -> {
                        if (old != null) {
                            old.addSuppressed(ex2);
                            return old;
                        }
                        return ex2;
                    });
                }
            }
            // Rethrow if present
            ex.ifNotNull(Exceptions::chuck);
        };
    }

    /**
     * Variant on <code>andThen</code> which guarantees that the passed Runnable
     * will be run <i>regardless of whether this one's run() method throws an
     * exception</i>, and which guarantees that the passed runable is run
     * <b>before</b>
     * this one's <code>run()</code> method - if both throw exceptions, the
     * first's exception will be added as a suppressed exception to the
     * second's. Offers the same functionality as
     * <code>andAlwaysFirst(ThrowingRunnable)</code> for <code>Runnables</code>
     * - named differently so lambda users will not have to cast as one or the
     * other.
     *
     * @param run Another runnable
     * @return a wrapper around this and the other
     */
    default ThrowingRunnable andAlwaysRunFirst(Runnable run) {
        return () -> {
            Obj<Throwable> ex = Obj.create();
            try {
                run.run();
            } catch (Exception ex1) {
                ex.set(ex1);
            } finally {
                try {
                    ThrowingRunnable.this.run();
                } catch (Exception | Error ex2) {
                    ex.apply(old -> {
                        if (old != null) {
                            old.addSuppressed(ex2);
                            return old;
                        }
                        return ex2;
                    });
                }
            }
            // Rethrow if present
            ex.ifNotNull(Exceptions::chuck);
        };
    }

    /**
     * Variant on <code>andAlwaysRun</code> which only executes the second
     * runnable if the passed BooleanSupplier returns true.
     *
     * @param run Another runnable
     * @return a wrapper around this and the other
     */
    default ThrowingRunnable andAlwaysIf(BooleanSupplier test, ThrowingRunnable run) {
        return () -> {
            Obj<Throwable> ex = Obj.create();
            try {
                ThrowingRunnable.this.run();
            } catch (Exception ex1) {
                ex.set(ex1);
            } finally {
                if (test.getAsBoolean()) {
                    try {
                        run.run();
                    } catch (Exception | Error ex2) {
                        ex.apply(old -> {
                            if (old != null) {
                                old.addSuppressed(ex2);
                                return old;
                            }
                            return ex2;
                        });
                    }
                }
            }
            // Rethrow if present
            ex.ifNotNull(Exceptions::chuck);
        };
    }

    /**
     * Variant on <code>andAlwaysRun</code> which guarantees that the passed
     * runnable will be run <i>regardless of whether this one's run() method
     * throws an exception</i> - if both throw exceptions, the second's
     * exception will be added as a suppressed exception to the first; only
     * executes the passed runnable if the passed supplier returns non-null at
     * the time run() is called.
     *
     * @param run Another runnable
     * @return a wrapper around this and the other
     */
    default ThrowingRunnable andAlwaysIfNotNull(Supplier<?> testForNull, ThrowingRunnable run) {
        return () -> {
            Obj<Throwable> ex = Obj.create();
            try {
                ThrowingRunnable.this.run();
            } catch (Exception ex1) {
                ex.set(ex1);
            } finally {
                if (testForNull.get() != null) {
                    try {
                        run.run();
                    } catch (Exception | Error ex2) {
                        ex.apply(old -> {
                            if (old != null) {
                                old.addSuppressed(ex2);
                                return old;
                            }
                            return ex2;
                        });
                    }
                }
            }
            // Rethrow if present
            ex.ifNotNull(Exceptions::chuck);
        };
    }

    /**
     * Chain this runnable with another, but only execute the passed one if the
     * Supplier passed returns non-null - useful for executing shutdown tasks
     * where if initialization failed, there is nothing to shut down.
     *
     * @param test
     * @param run
     * @return
     */
    default ThrowingRunnable andThenIfNotNull(Supplier<?> test, ThrowingRunnable run) {
        return () -> {
            ThrowingRunnable.this.run();
            if (test.get() != null) {
                run.run();
            }
        };
    }

    /**
     * Variant on andThen which only executes the passed runnable if the boolean
     * test returns true at the time of running.
     *
     * @param test A test
     * @param run A runnable
     * @return A wrapper around this and the passed runnable
     */
    default ThrowingRunnable andThenIf(BooleanSupplier test, ThrowingRunnable run) {
        return () -> {
            ThrowingRunnable.this.run();
            if (test.getAsBoolean()) {
                run.run();
            }
        };
    }

    default ThrowingRunnable andThen(Runnable run) {
        return () -> {
            ThrowingRunnable.this.run();
            run.run();
        };
    }

    default ThrowingRunnable andThen(Callable<Void> run) {
        return () -> {
            ThrowingRunnable.this.run();
            run.call();
        };
    }

    /**
     * To use a ThrowingRunnable in an api that calls for a supplier (frequently
     * one wants a single call that can sometimes needs a return value, and
     * sometimes doesn't.
     *
     * @return A ThrowingSupplier that runs this ThrowingRunnable and returns
     * null
     */
    default ThrowingSupplier<Void> toThrowingSupplier() {
        return () -> {
            this.run();
            return null;
        };
    }

    default <T> ThrowingSupplier<T> toThrowingSupplier(T obj) {
        return () -> {
            this.run();
            return obj;
        };
    }

    static ThrowingRunnable fromRunnable(Runnable run) {
        return new ThrowingRunnable() {

            public void run() throws Exception {
                run.run();
            }

            public String toString() {
                return run.toString();
            }
        };
    }

    default void addAsShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(toRunnable()));
    }

    public static ThrowingRunnable fromAutoCloseable(AutoCloseable e) {
        return e::close;
    }
}
