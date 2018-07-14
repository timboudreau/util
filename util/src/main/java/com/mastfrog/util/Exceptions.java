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
package com.mastfrog.util;

/**
 * Stub version of org.openide.util.Exceptions
 *
 * @author Tim Boudreau
 */
public final class Exceptions {
    private Exceptions() {}

    public static void printStackTrace(String msg, Throwable t) {
        com.mastfrog.util.preconditions.Exceptions.printStackTrace(msg, t);
    }

    public static void printStackTrace(Class<?> caller, String msg, Throwable t) {
        com.mastfrog.util.preconditions.Exceptions.printStackTrace(caller, msg, t);
    }

    public static void printStackTrace(Class<?> caller, Throwable t) {
        com.mastfrog.util.preconditions.Exceptions.printStackTrace(caller, t);
    }

    public static void printStackTrace(Throwable t) {
        com.mastfrog.util.preconditions.Exceptions.printStackTrace(t);
    }

    /**
     * Dirty trick to rethrow a checked exception.  Makes it possible to 
     * implement an interface such as Iterable (which cannot throw exceptions)
     * without the useless re-wrapping of exceptions in RuntimeException.
     * 
     * @param t A throwable.  This method will throw it without requiring a 
     * catch block.
     */
    public static <ReturnType> ReturnType chuck(Throwable t) {
        return com.mastfrog.util.preconditions.Exceptions.chuck(t);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void chuck(Class<T> type, Throwable t) throws T {
        com.mastfrog.util.preconditions.Exceptions.chuck(type, t);
    }
    
    @SuppressWarnings("ThrowableResultIgnored")
    public static <ReturnType> ReturnType chuckOriginal(Throwable t) {
        return com.mastfrog.util.preconditions.Exceptions.chuckOriginal(t);
    }
}
