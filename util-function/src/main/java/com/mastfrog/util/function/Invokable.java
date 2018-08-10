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
package com.mastfrog.util.function;

import java.util.concurrent.Callable;

/**
 * Callable-like construct which throws a typed exception and takes an argument.
 * Post Java-8, prefer java.util.function classes unless the ability to generically
 * type the thrown exception is actually useful.
 *
 * @author Tim Boudreau
 */
public abstract class Invokable<ArgType, ResultType, ExceptionType extends Exception> {

    private final Class<ExceptionType> thrown;

    protected Invokable(Class<ExceptionType> thrown) {
        this.thrown = thrown;
    }

    public static Invokable<?, ?, RuntimeException> wrap(final Runnable r) {
        return new Invokable<Void, Void, RuntimeException>() {
            @Override
            public Void run(Void argument) throws RuntimeException {
                r.run();
                return null;
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected Invokable() {
        //Have to bypass generics for this
        Class c = RuntimeException.class;
        this.thrown = c;
    }

    public abstract ResultType run(ArgType argument) throws ExceptionType;

    public final Class<ExceptionType> thrown() {
        return thrown;
    }

    public final ThrowingFunction<ArgType, ResultType> toFunction() {
        return this::run;
    }

    public Callable<ResultType> toCallable() {
        return new Callable<ResultType>() {
            @Override
            public ResultType call() throws Exception {
                return run(null);
            }
        };
    }

    public Callable<ResultType> toCallable(final ArgType arg) {
        return new Callable<ResultType>() {
            @Override
            public ResultType call() throws Exception {
                return run(arg);
            }
        };
    }

    public Runnable toRunnable(final ArgType arg) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Invokable.this.run(arg);
                } catch (Exception ex) {
                    com.mastfrog.util.preconditions.Exceptions.chuck(ex);
                }
            }
        };
    }
}
