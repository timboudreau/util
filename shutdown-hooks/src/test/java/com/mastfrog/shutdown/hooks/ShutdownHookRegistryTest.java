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
package com.mastfrog.shutdown.hooks;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.mastfrog.function.state.Obj;
import com.mastfrog.shutdown.hooks.ShutdownHookRegistry.VMShutdownHookRegistry;
import com.mastfrog.util.collections.ArrayUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntFunction;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ShutdownHookRegistryTest {

    @Test
    public void testShutdownHooksAreRunInReverseOrder() throws IOException {
        Injector deps = Guice.createInjector(new M());
        ShutdownHookRegistry hooks = deps.getInstance(ShutdownHookRegistry.class);

        ThingOne one = deps.getInstance(ThingOne.class);
        ThingTwo two = deps.getInstance(ThingTwo.class);
        ThingThree three = deps.getInstance(ThingThree.class);
        ThingThree three2 = deps.getInstance(ThingThree.class);
        assertSame(three, three2);

        assertSame(ShutdownHookRegistry.current().get(), hooks,
                "Should have gotten current instance");

        hooks.shutdown();
        
        assertFalse(ShutdownHookRegistry.current().isPresent());

        assertTrue(one.hookRan);
        assertTrue(two.hookRan);
        assertTrue(three.hookRan);

        assertEquals(3, order.size());

        assertEquals(order, Arrays.asList(3, 2, 1));
    }

    @Test
    public void testReentrantHooks() throws Exception {
        HFact f = new HFact();
        Hks hooks = new Hks();
        hooks.add(f.addReentrant(hooks, 11, 15));
        hooks.add(f.addReentrant(hooks, 1, 5));

        hooks.close();
    }

    @Test
    public void testLayerOrder() throws Exception {
        HFact f = new HFact();
        Hks hooks = new Hks();
        hooks.addFirst(f.apply(12));
        hooks.addFirst(f.apply(11));
        hooks.addFirst(f.apply(10));
        hooks.add(f.apply(22));
        hooks.add(f.error());
        hooks.add(f.apply(21));
        hooks.add(f.apply(20));
        hooks.add(f.runtimeException());
        hooks.addLast(f.apply(32));
        hooks.addLast(f.apply(31));
        hooks.addLast(f.apply(30));
        Obj<Throwable> th = Obj.create();
        int count = hooks.internalRunShutdownHooks(th);

        f.assertAllExecuted().assertOrder(10, 11, 12, 20, 21, 22, 32, 31, 30);
        Throwable thrown = th.get();
        assertNotNull(thrown, "Nothing thrown");
        assertTrue(
                thrown instanceof RuntimeException, "The first-thrown runtime exception should have been rethrown");
        assertNotNull(thrown.getSuppressed());
        assertEquals(1, thrown.getSuppressed().length, "A suppressed exception should be present");
        assertTrue(thrown.getSuppressed()[0] instanceof Error, "Suppressed exception should be the second-thrown error");
        assertEquals(count, f.added.size() + 2, "Wrong number of hook runs reported");
    }

    static class Hks extends ShutdownHookRegistry implements AutoCloseable {

        @Override
        public void close() throws Exception {
            super.runShutdownHooks();
        }
    }

    static class HFact implements IntFunction<Runnable> {

        private final List<Integer> all = new ArrayList<>();
        private final Set<Integer> added = new TreeSet<>();

        HFact assertOrder(int... vals) {
            List<Integer> got = ArrayUtils.toBoxedList(vals);
            assertEquals(all, got);
            return this;
        }

        HFact assertAllExecuted() {
            Set<Integer> found = new TreeSet<>(all);
            assertEquals(found, added, "Set of hooks run does not match those added");
            return this;
        }

        public Runnable runtimeException() {
            return () -> {
                throw new RuntimeException("Uh oh.");
            };
        }

        public Runnable error() {
            return () -> {
                throw new Error("Oh no!");
            };
        }

        @Override
        public Runnable apply(int index) {
            added.add(index);
            return () -> {
                all.add(index);
            };
        }

        public Runnable addReentrant(ShutdownHookRegistry reg, int a, int b) {
            return () -> {
                all.add(a);
                reg.add(apply(b));
            };
        }
    }

    static class M extends AbstractModule {

        @Override
        protected void configure() {
            bind(ShutdownHookRegistry.class).to(VMShutdownHookRegistry.class).asEagerSingleton();
            bind(ThingOne.class).in(Scopes.SINGLETON);
            bind(ThingTwo.class).in(Scopes.SINGLETON);
            bind(ThingThree.class).in(Scopes.SINGLETON);
        }

    }

    private static final List<Integer> order = new ArrayList<>();

    public static final class ThingOne implements Runnable {

        private boolean hookRan;

        @Inject
        ThingOne(ShutdownHookRegistry reg) {
            reg.add(this);
        }

        @Override
        public void run() {
            hookRan = true;
            order.add(1);
        }
    }

    public static final class ThingTwo implements Runnable {

        private boolean hookRan;

        @Inject
        ThingTwo(ShutdownHookRegistry reg) {
            reg.add(this);
        }

        @Override
        public void run() {
            hookRan = true;
            order.add(2);
        }
    }

    public static final class ThingThree implements Runnable {

        boolean hookRan;

        @Inject
        ThingThree(ShutdownHookRegistry reg) {
            reg.add(this);
        }

        @Override
        public void run() {
            hookRan = true;
            order.add(3);
        }
    }

}
