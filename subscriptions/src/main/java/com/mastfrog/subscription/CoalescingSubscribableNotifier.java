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
package com.mastfrog.subscription;

import com.mastfrog.util.collections.MapFactories;
import com.mastfrog.util.collections.IntMap;
import com.mastfrog.util.collections.MapFactory;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import static com.mastfrog.subscription.DeliveryStrategy.COALESCING;

/**
 * Delivers events after a delay which is reset when new events occur, only
 * delivering the most recent one at the time the delay expires.
 *
 * @author Tim Boudreau
 */
class CoalescingSubscribableNotifier<K, E> implements SubscribableNotifier<K, E> {

    private final ScheduledExecutorService svc;
    private final IntSupplier delay;
    private final EventMap<K, SchedulableRunner<K, E>> pending;
    private final TimeUnit unit;
    private final MapFactory cacheType;
    private final SubscribableNotifier<K, E> delegate;

    CoalescingSubscribableNotifier(ScheduledExecutorService svc,
            MapFactory cacheType, int delay, TimeUnit unit,
            SubscribableNotifier<K, E> delegate) {
        this(svc, cacheType, () -> delay, unit, delegate);
    }

    CoalescingSubscribableNotifier(ScheduledExecutorService svc,
            MapFactory cacheType, IntSupplier delay, TimeUnit unit,
            SubscribableNotifier<K, E> delegate) {
        this.svc = svc;
        this.delay = delay;
        this.unit = unit;
        this.cacheType = cacheType;
        this.delegate = delegate;
        if (cacheType == MapFactories.IDENTITY_WITHOUT_REFERENCE) {
            pending = new IdentityHashEventMap<>();
        } else {
            pending = new MapEventMap<>(cacheType.createMap(64, false));
        }
    }

    @Override
    public SubscribableNotifier<K, E> coalescing(ScheduledExecutorService svc,
            MapFactory cacheType, int delay, TimeUnit unit) {
        throw new IllegalStateException("Already a coalescing notifier");
    }

    @Override
    public SubscribableNotifier<K, E> async(Executor exe) {
        throw new IllegalStateException("Coalescing notifier is already async");
    }

    @Override
    public DeliveryStrategy deliveryStrategy() {
        return COALESCING;
    }

    private synchronized SchedulableRunner<K, E> runner(K key) {
        SchedulableRunner<K, E> runner = pending.get(key);
        if (runner == null) {
            runner = (cacheType == MapFactories.IDENTITY_WITHOUT_REFERENCE
                    ? new DelayedRunner(key)
                    : new KeyHoldingRunner());
            pending.put(key, runner);
        }
        return runner;
    }

    private synchronized void removeRunner(K k, KeyHoldingRunner runner) {
        SchedulableRunner<K, E> current = pending.get(k);
        if (current == runner) {
            pending.remove(k);
        }
    }

    @Override
    public void onEvent(K key, E event) {
        runner(key).schedule(key, event);
    }

    interface SchedulableRunner<K, E> {

        void cancel();

        void schedule(K k, E e);
    }

    class DelayedRunner implements Runnable, SchedulableRunner<K, E> {

        private final K key;
        private final AtomicReference<E> event = new AtomicReference<>();
        private ScheduledFuture<?> future;

        DelayedRunner(K key) {
            this.key = key;
        }

        public synchronized void cancel() {
            if (future != null) {
                future.cancel(false);
            }
        }

        public void schedule(K k, E e) {
            if (svc.isShutdown()) {
                delegate.onEvent(key, e);
                return;
            }
            event.set(e);
            synchronized (this) {
                if (future != null && !future.isDone()) {
                    future.cancel(false);
                }
                int del = delay.getAsInt();
                future = svc.schedule(this, del, unit);
            }
        }

        @Override
        public void run() {
            E obj = event.getAndSet(null);
            synchronized (this) {
                future = null;
            }
            delegate.onEvent(key, obj);
        }
    }

    private static final class Pair<K, E> {

        private final K key;
        private final E event;

        public Pair(K key, E event) {
            this.key = key;
            this.event = event;
        }

    }

    class KeyHoldingRunner implements Runnable, SchedulableRunner<K, E> {

        private final AtomicReference<Pair<K, E>> event = new AtomicReference<>();
        private Future<?> future;

        public synchronized void cancel() {
            if (future != null) {
                future.cancel(false);
            }
        }

        public void schedule(K k, E e) {
            if (svc.isShutdown()) {
                delegate.onEvent(k, e);
                return;
            }
            event.set(new Pair<>(k, e));
            synchronized (this) {
                if (future != null) {
                    future.cancel(false);
                }
                future = svc.schedule(this, delay.getAsInt(), unit);
            }
        }

        @Override
        public void run() {
            Pair<K, E> obj = event.getAndSet(null);
//            removeRunner(obj.key, this);
            delegate.onEvent(obj.key, obj.event);
        }
    }

    static final class MapEventMap<K, E> implements EventMap<K, E> {

        private final Map<K, E> map;

        public MapEventMap(Map<K, E> map) {
            this.map = map;
        }

        @Override
        public void put(K k, E e) {
            map.put(k, e);
        }

        @Override
        public void remove(K k) {
            map.remove(k);
        }

        @Override
        public E get(K k) {
            return map.get(k);
        }
    }

    static final class IdentityHashEventMap<K, E> implements EventMap<K, E> {

        private final IntMap<E> map;

        public IdentityHashEventMap() {
            this.map = IntMap.create(25);
        }

        @Override
        public void put(K k, E e) {
            map.put(System.identityHashCode(k), e);
        }

        @Override
        public void remove(K k) {
            map.remove(System.identityHashCode(k));
        }

        @Override
        public E get(K k) {
            return map.get(System.identityHashCode(k));
        }
    }
}
