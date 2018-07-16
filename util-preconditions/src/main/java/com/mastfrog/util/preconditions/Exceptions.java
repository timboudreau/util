/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
package com.mastfrog.util.preconditions;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple utilities for dealing with exceptions.
 *
 * @author Tim Boudreau
 */
public final class Exceptions {

    private Exceptions() {
    }

    private static final class DefaultExceptionHandler implements ExceptionHandler {
    }

    static final ExceptionHandler HANDLER;

    static {
        Iterator<ExceptionHandler> e = ServiceLoader.load(ExceptionHandler.class).iterator();
        HANDLER = e.hasNext() ? e.next() : new DefaultExceptionHandler();
    }

    public static void printStackTrace(String msg, Throwable t) {
        HANDLER.printStackTrace(msg, t);
    }

    public static void printStackTrace(Class<?> caller, String msg, Throwable t) {
        HANDLER.printStackTrace(caller, msg, t);
    }

    public static void printStackTrace(Class<?> caller, Throwable t) {
        HANDLER.printStackTrace(caller, t);
    }

    public static void printStackTrace(Throwable t) {
        HANDLER.printStackTrace(t);
    }

    /**
     * Dirty trick to rethrow a checked exception. Makes it possible to
     * implement an interface such as Iterable (which cannot throw exceptions)
     * without the useless re-wrapping of exceptions in RuntimeException.
     *
     * @param t A throwable. This method will throw it without requiring a catch
     * block.
     */
    public static <ReturnType> ReturnType chuck(Throwable t) {
        chuck(RuntimeException.class, t);
        throw new AssertionError(t); //should not get here
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void chuck(Class<T> type, Throwable t) throws T {
        throw (T) t;
    }

    public static <ReturnType> ReturnType chuckUnless(Throwable t, ReturnType what, BooleanSupplier predicate) {
        if (!predicate.getAsBoolean()) {
            return chuck(t);
        }
        return what;
    }

    /**
     * Unwinds any causing-exception and chucks the root cause throwable.
     *
     * @param <ReturnType> The type to pretend to return (this method always
     * exits abnormally), so methods can be written cleanly, e.g.
     * <code>return Exceptions.chuck(someException).
     * @param t
     * @return
     */
    @SuppressWarnings("ThrowableResultIgnored")
    public static <ReturnType> ReturnType chuckOriginal(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        chuck(RuntimeException.class, t);
        throw new AssertionError(t); //should not get here
    }

    /**
     * Service provider which logs exceptions; the default implementation uses
     * the JDK's logger.
     */
    public interface ExceptionHandler {

        default void printStackTrace(String msg, Throwable t) {
            Logger.getLogger(Exceptions.class.getName()).log(Level.SEVERE, msg, t);
        }

        default void printStackTrace(Class<?> caller, String msg, Throwable t) {
            Logger.getLogger(caller.getName()).log(Level.SEVERE, msg, t);
        }

        default void printStackTrace(Class<?> caller, Throwable t) {
            Logger.getLogger(caller.getName()).log(Level.SEVERE, null, t);
        }

        default void printStackTrace(Throwable t) {
            Logger.getLogger(Exceptions.class.getName()).log(Level.SEVERE, null, t);
        }
    }
}
