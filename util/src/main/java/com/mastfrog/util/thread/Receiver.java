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
package com.mastfrog.util.thread;

import static com.mastfrog.util.preconditions.Checks.notNull;

/**
 * Callback which can be passed some object which cannot be computed on the
 * calling thread.
 * <p/>
 * Typically the object which is passed to a Receiver is not meant to be touched
 * except from within the receive() method; implementations may proxy an
 * interface to one which will check that the correct locks are held and throw
 * an exception otherwise.
 *
 * @author Tim Boudreau
 */
public abstract class Receiver<T> {

    public abstract void receive(T object);

    public <E extends Throwable> void onFail(E exception) throws E {
        throw exception;
    }

    public void onFail() {
        //do nothing
        System.err.println(this + " failed");
    }

    public static <T> Receiver<T> of(Callback<T> callback) {
        return new CallbackReceiver<>(notNull("callback", callback));
    }

    private static final class CallbackReceiver<T> extends Receiver<T> {

        private final Callback<T> callback;

        public CallbackReceiver(Callback<T> callback) {
            this.callback = callback;
        }

        @Override
        public void receive(T object) {
            callback.receive(null, object);
        }

        @Override
        public <E extends Throwable> void onFail(E exception) throws E {
            callback.receive(exception, null);
        }

        @Override
        public void onFail() {
            callback.receive(new Exception("Unknown failure"), null);
        }
    }
}
