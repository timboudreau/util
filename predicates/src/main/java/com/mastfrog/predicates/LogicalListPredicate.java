/*
 * The MIT License
 *
 * Copyright 2019 Tim Boudreau.
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
package com.mastfrog.predicates;

import com.mastfrog.abstractions.AbstractNamed;
import com.mastfrog.abstractions.Named;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * Keeps stack depth from going insane, and provides pretty formatting for
 * toString().
 *
 * @author Tim Boudreau
 */
final class LogicalListPredicate<T> extends AbstractNamed implements NamedPredicate<T>, ListPredicate<T> {

    private final List<Predicate<? super T>> l = new ArrayList<>(5);
    private final boolean and;

    LogicalListPredicate(boolean and, Predicate<? super T> first) {
        this(and);
        accept(first);
    }

    LogicalListPredicate(boolean and) {
        this.and = and;
    }

    @Override
    public Iterator<Predicate<? super T>> iterator() {
        return l.iterator();
    }

    void add(Predicate<T> pred) {
        l.add(pred);
    }

    @Override
    public NamedPredicate<T> and(Predicate<? super T> other) {
        if (and) {
            accept(other);
            return this;
        } else {
            return Predicates.andPredicate(this).and(other);
        }
    }

    @Override
    public NamedPredicate<T> or(Predicate<? super T> other) {
        if (!and) {
            accept(other);
            return this;
        } else {
            return Predicates.orPredicate(this).or(other);
        }
    }

    static final Consumer<IntConsumer> ENTRY_COUNT = ThreadLocalUtils.entryCounter();

    @Override
    public String name() {
        if (l.isEmpty()) {
            return "empty";
        }
        StringBuilder sb = new StringBuilder();
        ENTRY_COUNT.accept(depth -> {
            char[] indent = new char[depth * 2];
            for (Iterator<?> it = l.iterator(); it.hasNext();) {
                Object next = it.next();
                String name = Named.findName(next);
                if (!(next instanceof LogicalListPredicate<?>)) {
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                        sb.append('\n');
                    }
                    sb.append(indent);
                }
                sb.append(name);
                if (it.hasNext()) {
                    sb.append(and ? " &&" : " ||");
                }
            }
        });
        return sb.toString();
    }

    @Override
    public boolean test(T t) {
        boolean result = true;
        List<Predicate<? super T>> copy = new ArrayList<>(l);
        for (Predicate<? super T> p : copy) {
            if (and) {
                result &= p.test(t);
            } else {
                result |= p.test(t);
            }
            if (and && !result) {
                break;
            }
        }
        return result;
    }

    @Override
    public void accept(Predicate<? super T> t) {
        l.add(t);
    }
}
