/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.converters;

import com.mastfrog.graph.IntGraph;
import com.mastfrog.graph.ObjectPath;
import com.mastfrog.graph.ObjectGraph;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * A collection of conversion functions from type to type, which is capable of
 * synthesizing a conversion function by chaining together several conversions
 * if needed. Input types may be subtypes of the registered type. Uses the graph
 * library to provide some graph-theoretic rigor around efficiently computing
 * paths through the sets of registered converters to efficiently find the
 * shortest conversion path for non-trivial conversions.
 *
 * @author Tim Boudreau
 */
public final class Converters {

    private final List<ConverterEntry<?, ?>> entries = new CopyOnWriteArrayList<>();
    private volatile ObjectGraph<Class<?>> graph;

    /**
     * Register a conversion function.
     *
     * @param <F> The input type
     * @param <T> The output type
     * @param from The input type
     * @param to The output type
     * @param converter The conversion function
     */
    public <F, T> void register(Class<F> from, Class<T> to,
            Function<F, T> converter) {
        synchronized (this) {
            graph = null;
            entries.add(new ConverterEntry<>(
                    notNull("from", from),
                    notNull("to", to),
                    notNull("convertFrom", converter))
            );
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public String toString() {
        return graph().toString();
    }

    /**
     * Convert the input object to the output type if possible; the input type
     * may be a subtype of the registered conversion function.
     *
     * @param <F> The input type
     * @param <T> The output type
     * @param from The input object
     * @param to The desired output type
     * @return An object representing in input converted to the output type, or
     * null if no conversion path resulting in the output type is possible, or
     * if some registered converter along every path returned null at some step
     * of the conversion
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <F, T> T convert(F from, Class<T> to) {
        if (from == null) {
            return null;
        }
        Function converter = converter(from.getClass(), to);
        if (converter == null) {
            return null;
        }
        return to.cast(converter.apply(from));
    }

    @SuppressWarnings("unchecked")
    public <F, T> Class<? super F> convertingAs(F from, Class<T> to) {
        List<ObjectPath<Class<?>>> paths = paths(from.getClass(), to);
        if (!paths.isEmpty()) {
            return (Class<? super F>) paths.get(0).first();
        }
        return null;
    }

    public <F, T> boolean canConvert(F from, Class<T> to) {
        boolean result = !paths(from.getClass(), to).isEmpty();
        if (!result) {
            for (Class<?> type : compatibleTypes(from.getClass())) {
                result = !paths(type, to).isEmpty();
                if (result) {
                    break;
                }
            }
        }
        return result;
    }

    public <F, T> boolean hasConverter(Class<F> from, Class<T> to) {
        return !paths(from.getClass(), to).isEmpty();
    }

    @SuppressWarnings("unchecked")
    public <T> Set<Class<? super T>> compatibleTypes(T obj) {
        Set<Class<? super T>> types = new HashSet<>();
        Class<?> specificType = obj.getClass();
        for (ConverterEntry<?, ?> e : entries) {
            if (e.from.isAssignableFrom(specificType)) {
                types.add((Class<? super T>) e.from);
            }
            if (e.to.isAssignableFrom(specificType)) {
                types.add((Class<? super T>) e.to);
            }
        }
        return types;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <F, T> List<ObjectPath<Class<?>>> paths(Class<F> from, Class<T> to) {
        List<ObjectPath<Class<?>>> paths = graph().pathsBetween(from, to);
        if (paths.isEmpty()) {
            for (ConverterEntry<?, ?> ce : entries) {
                if (ce.from.isAssignableFrom(from)) {
                    paths = graph().pathsBetween(ce.from, to);
                    if (!paths.isEmpty()) {
                        break;
                    }
                }
            }
        }
        return paths;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <F, T> Function<? super F, ? extends T> converter(Class<F> from, Class<T> to) {
        if (from == to) {
            return IdentityFunction.INSTANCE;
        }
        List<ObjectPath<Class<?>>> pathsBetween = paths(from, to);
        if (pathsBetween.isEmpty()) {
            return null;
        }
        return new ObjectPathConverter(pathsBetween);
//        ObjectPath<Class<?>> path = pathsBetween.get(0);
//        System.out.println("OBJECT PATHS: " + path);
//        Iterator<Class<?>> iter = path.iterator();
//        Class<?> first = iter.next();
//        assert from == first;
//        Function<F, ?> result = f(from, iter.next(), iter);
//        return (Function<F, T>) result;
    }

    static class IdentityFunction implements Function {

        private static final IdentityFunction INSTANCE = new IdentityFunction();

        @Override
        public Object apply(Object t) {
            return t;
        }
    }

    /**
     * This is ugly, but both more efficient and allows for easily iterating all
     * conversion paths if some may be intermittently available.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    final class ObjectPathConverter implements Function {

        private final List<ObjectPath<Class<?>>> paths;

        public ObjectPathConverter(List<ObjectPath<Class<?>>> paths) {
            this.paths = paths;
        }

        @Override
        public String toString() {
            return paths.toString();
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Object apply(Object t) {
            Iterator<ObjectPath<Class<?>>> it = paths.iterator();
            while (it.hasNext()) {
                ObjectPath<Class<?>> currPath = it.next();
                Iterator<Class<?>> typesIt = currPath.iterator();
                if (!typesIt.hasNext()) {
                    continue;
                }
                Class<?> lastType = typesIt.next();
                if (!typesIt.hasNext()) {
                    if (lastType.isInstance(t)) {
                        return t;
                    }
                }
                Object curr = t;
                while (typesIt.hasNext()) {
                    Class<?> currType = typesIt.next();
                    Function converter = find(lastType, currType);
                    if (converter == null) {
                        break;
                    }
                    curr = converter.apply(curr);
                    if (curr == null) {
                        break;
                    }
                    lastType = currType;
                    if (!typesIt.hasNext()) {
                        return curr;
                    }
                }
            }
            return null;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 73 * hash + Objects.hashCode(this.paths);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ObjectPathConverter other = (ObjectPathConverter) obj;
            return Objects.equals(this.paths, other.paths);
        }
    }

    /*
    private <A, B> Function<A, ?> f(Class<A> a, Class<B> b, Iterator<Class<?>> iter) {
        ConverterEntry<A, B> c = find(a, b);
        if (!iter.hasNext()) {
            return c;
        }
        return x(c, c.to, iter.next(), iter);
    }

    private <P, A, B> Function<P, ?> x(Function<P, A> func, Class<A> a, Class<B> b, Iterator<Class<?>> iter) {
        ConverterEntry<A, B> c = find(a, b);
        if (c == null) {
            throw new IllegalStateException("Null for " + a.getSimpleName() + " and " + b.getSimpleName());
        }
        Function<P, B> result = func.andThen(c);
        if (iter.hasNext()) {
            Class<?> nxt = iter.next();
            Function<P, ?> rs = x(result, c.to, nxt, iter);
            return rs;
        }
        return result;
    }
     */
    @SuppressWarnings("unchecked")
    private <F, T> ConverterEntry<F, T> find(Class<F> from, Class<T> to) {
        for (ConverterEntry<?, ?> e : entries) {
            if (e.from == from && e.to == to) {
                return (ConverterEntry<F, T>) e;
            }
        }
        return null;
    }

    private ObjectGraph<Class<?>> graph() {
        ObjectGraph<Class<?>> g = this.graph;
        if (g == null) {
            synchronized (this) {
                g = this.graph;
                if (g == null) {
                    this.graph = g = buildGraph();
                }
                return g;
            }
        }
        return g;
    }

    private ObjectGraph<Class<?>> buildGraph() {
        Set<Class<?>> types = new HashSet<>();
        for (ConverterEntry<?, ?> c : entries) {
            types.add(c.from);
            types.add(c.to);
        }
        List<Class<?>> sorted = new ArrayList<>(types);
        Collections.sort(sorted, (a, b) -> {
            return a.getName().compareTo(b.getName());
        });
        BitSet[] outEdges = new BitSet[sorted.size()];
        BitSet[] inEdges = new BitSet[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            outEdges[i] = new BitSet(sorted.size());
            inEdges[i] = new BitSet(sorted.size());
        }
        for (ConverterEntry<?, ?> c : entries) {
            assert c != null;
            int fix = sorted.indexOf(c.from);
            int tix = sorted.indexOf(c.to);
            outEdges[fix].set(tix);
            inEdges[tix].set(fix);
        }
        IntGraph g = IntGraph.create(outEdges, inEdges);
        ObjectGraph<Class<?>> classGraph = g.toObjectGraph(sorted);
        return classGraph;
    }

    static final class ConverterEntry<F, T> implements Function<F, T> {

        private final Class<F> from;
        private final Class<T> to;
        private final Function<F, T> converter;

        ConverterEntry(Class<F> from, Class<T> to, Function<F, T> converter) {
            this.from = from;
            this.to = to;
            this.converter = converter;
        }

        @Override
        public T apply(F f) {
            return converter.apply(f);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 59 * hash + Objects.hashCode(this.from);
            hash = 59 * hash + Objects.hashCode(this.to);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ConverterEntry<?, ?> other = (ConverterEntry<?, ?>) obj;
            if (!Objects.equals(this.from, other.from)) {
                return false;
            }
            return Objects.equals(this.to, other.to);
        }

        @Override
        public <V> Function<F, V> andThen(Function<? super T, ? extends V> after) {
            return new ComposedFunction<>(this, after); // loggability
        }

        @Override
        public <V> Function<V, T> compose(Function<? super V, ? extends F> before) {
            return new ComposedFunction<>(before, this); // loggability
        }

        private String nameOf(Class<?> type) {
            if (type.isArray()) {
                return type.getComponentType().getSimpleName() + "[]";
            }
            return type.getSimpleName();
        }

        @Override
        public String toString() {
            return "(" + nameOf(from) + "->" + nameOf(to) + ')';
        }
    }

    static final class ComposedFunction<P, F, T> implements Function<P, T> {

        private final Function<? super P, ? extends F> pre;
        private final Function<? super F, ? extends T> post;

        public ComposedFunction(Function<? super P, ? extends F> pre, Function<? super F, ? extends T> post) {
            this.pre = pre;
            this.post = post;
        }

        @Override
        public T apply(P t) {
            return post.apply(pre.apply(t));
        }

        @Override
        public String toString() {
            return "(" + pre + "->" + post + ')';
        }
    }
}
