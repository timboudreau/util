/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.function.state;

import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
final class ObjImpl<T> implements Obj<T> {

    private T obj;

    ObjImpl() {

    }

    ObjImpl(T initial) {
        this.obj = initial;
    }

    @Override
    public T get() {
        return obj;
    }

    @Override
    public T set(T obj) {
        T old = this.obj;
        this.obj = obj;
        return old;
    }

    @Override
    public boolean ifUpdate(T newValue, Runnable ifChange) {
        if (!Objects.equals(obj, newValue)) {
            obj = newValue;
            ifChange.run();
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return Objects.toString(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(obj);
    }

    @Override
    public boolean equals(Object o) {
        return o == this ? true : o instanceof Obj && Objects.equals(obj,
                ((Obj) o).get());
    }
}
