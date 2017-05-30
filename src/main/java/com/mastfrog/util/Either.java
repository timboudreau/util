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

package com.mastfrog.util;

/**
 * Optional-like wrapper where a value may be one of two types.
 *
 * @author Tim Boudreau
 */
public class Either<A,B> {

    private final Class<B> bType;
    private final Class<A> aType;
    private A a;
    private B b;
    
    public Either(Class<A> aType, Class<B> bType) {
        this.aType = aType;
        this.bType = bType;
    }
    
    public void set(Object o) {
        if (o == null) {
            a = null;
            b = null;
        } else if (aType.isInstance(o)) {
            a = aType.cast(o);
            b = null;
        } else if (bType.isInstance(o)) {
            b = bType.cast(o);
            a = null;
        } else {
            throw new ClassCastException (o + " is neither " + aType.getName() + " nor " + bType.getName());
        }
    }
    
    public boolean isPresent() {
        return a != null || b != null;
    }

    public boolean isA() {
        return a != null;
    }
    
    public boolean isB() {
        return  b != null;
    }
    
    public void setA(A a) {
        set(a);
    }
    
    public void setB(B b) {
        set(b);
    }
    
    public A getA() {
        return a;
    }
    
    public B getB() {
        return b;
    }
}
