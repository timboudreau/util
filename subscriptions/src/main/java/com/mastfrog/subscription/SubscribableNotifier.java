package com.mastfrog.subscription;

import com.mastfrog.util.collections.MapFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Delivers events to be published by a Subscribable.
 *
 * @author Tim Boudreau
 */
public interface SubscribableNotifier<K, E> {

    /**
     * Publish one event to subscribers to one key; the delivery mechanism
     * (synchronous, asynchronous, coalesced, filtered, or something you
     * implemented) is determined by the instances which wrap this one on the
     * way to delivering the event.
     *
     * @param key The key object
     * @param event The event
     */
    void onEvent(K key, E event);

    default DeliveryStrategy deliveryStrategy() {
        return DeliveryStrategy.SYNCHRONOUS;
    }

    default SubscribableNotifier<K, E> async() {
        return async(ForkJoinPool.commonPool());
    }

    default SubscribableNotifier<K, E> async(BooleanSupplier whenTrue) {
        return async(ForkJoinPool.commonPool(), whenTrue);
    }

    default SubscribableNotifier<K, E> async(Executor exe, BooleanSupplier whenTrue) {
        switch (deliveryStrategy()) {
            case ASYNCHRONOUS:
            case COALESCING:
                throw new IllegalStateException("Already asynchronous or coalescing-async");
        }
        return new SubscribableNotifier<K, E>() {
            @Override
            public void onEvent(K k, E e) {
                if (whenTrue.getAsBoolean()) {
                    exe.execute(() -> {
                        SubscribableNotifier.this.onEvent(k, e);
                    });
                } else {
                    SubscribableNotifier.this.onEvent(k, e);
                }
            }

            @Override
            public DeliveryStrategy deliveryStrategy() {
                return DeliveryStrategy.ASYNCHRONOUS;
            }
        };
    }

    default SubscribableNotifier<K, E> async(Executor exe) {
        switch (deliveryStrategy()) {
            case ASYNCHRONOUS:
            case COALESCING:
                throw new IllegalStateException("Already asynchronous or coalescing-async");
        }
        return new SubscribableNotifier<K, E>() {
            @Override
            public void onEvent(K k, E e) {
                exe.execute(() -> {
                    SubscribableNotifier.this.onEvent(k, e);
                });
            }

            @Override
            public DeliveryStrategy deliveryStrategy() {
                return DeliveryStrategy.ASYNCHRONOUS;
            }
        };
    }

    default SubscribableNotifier<K, E> coalescing(ScheduledExecutorService svc, MapFactory cacheType, int delay, TimeUnit unit) {
        return new CoalescingSubscribableNotifier<>(svc, cacheType, delay, unit, this);
    }

    default SubscribableNotifier<K, E> coalescing(BooleanSupplier test, ScheduledExecutorService svc, MapFactory cacheType, int delay, TimeUnit unit) {
        CoalescingSubscribableNotifier<K,E> coa = new CoalescingSubscribableNotifier<>(svc, cacheType, delay, unit, this);
        return (K key, E event) -> {
            if (test.getAsBoolean()) {
                coa.onEvent(key, event);
            } else {
                this.onEvent(key, event);
            }
        };
    }

}
