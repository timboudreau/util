/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.subscription;

import com.mastfrog.util.collections.MapFactories;
import com.mastfrog.util.collections.SetFactory;
import com.mastfrog.util.collections.SetFactories;
import com.mastfrog.function.TriConsumer;
import com.mastfrog.util.collections.MapFactory;
import com.mastfrog.util.preconditions.Checks;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builds a Subscribable with the key retention and subscriber policy, dispatch
 * mechanism you specify.  Subscribable basically implements the pattern of
 * a one-to-many mapping between objects and types which are interested in
 * "events" that somehow relate to those objects.  By separating the way
 * keys are stored, subscribers for keys are stored, event dispatch and
 * delivery, it is possible to implement nearly any variant on the listener
 * pattern;  no assumptions are made about what listeners, or objects or
 * events are - you stitch that contract together with the functions and
 * map and set factories you provide while building it.  It is possible to
 * have multiple key types (ex. java.io.File and java.nio.Path) which can
 * be used interchangably - just choose one type to be the proimary key type,
 * and provide a KeyFactory to convert the other into it.
 * <p>
 * It's very abstract, but darned useful.
 *
 * @author Tim Boudreau
 */
public final class SubscribableBuilder {

    public static <K> KeyedSubscribableBuilder<K, K> withKeys(Class<K> keyType) {
        return withKeys(keyType, KeyFactory.identity());
    }

    public static <K, IK> KeyedSubscribableBuilder<K, IK> withKeys(Class<K> keyType, KeyFactory<? super K, ? extends IK> keys) {
        return new KeyedSubscribableBuilder<>(keyType, keys);
    }

    public static final class KeyedSubscribableBuilder<K, IK> {

        private final KeyFactory<? super K, ? extends IK> keys;
        private final Class<K> keyType;

        KeyedSubscribableBuilder(Class<K> keyType, KeyFactory<? super K, ? extends IK> keys) {
            this.keys = keys;
            this.keyType = keyType;
        }

        @SuppressWarnings("unchecked")
        public <K2> KeyedSubscribableBuilder<Object, IK> andKeys(Class<K2> additionalKeyType, KeyFactory<? super K2, ? extends IK> moreKeys) {
            if (keys instanceof MultiTypeKeyFactory<?>) {
                MultiTypeKeyFactory<IK> mk = (MultiTypeKeyFactory<IK>) keys;
                mk.add(additionalKeyType, moreKeys);
                return (KeyedSubscribableBuilder<Object, IK>) this;
            } else {
                MultiTypeKeyFactory<IK> result = new MultiTypeKeyFactory<>();
                result.add(keyType, keys);
                result.add(additionalKeyType, moreKeys);
                return new KeyedSubscribableBuilder<>(Object.class, result);
            }
        }

        public <E> EventSubscribableBuilder<K, IK, E, Consumer<? super E>> consumers() {
            return withEventApplier(EventApplier.consumers());
        }

        public <E> EventSubscribableBuilder<K, IK, E, BiConsumer<? super IK, ? super E>> biconsumers() {
            EventApplier<IK, E, BiConsumer<? super IK, ? super E>> app = EventApplier.biconsumers();
            return withEventApplier(app);
        }

        public <E, C> EventSubscribableBuilder<K, IK, E, C> withEventApplier(EventApplier<? super IK, ? super E, ? super C> applier) {
            return new EventSubscribableBuilder<>(keyType, keys, applier);
        }

    }

    public static final class EventSubscribableBuilder<K, IK, E, C> {

        private final Class<K> keyType;
        private final KeyFactory<? super K, ? extends IK> keys;
        private final EventApplier<? super IK, ? super E, ? super C> applier;

        EventSubscribableBuilder(
                Class<K> keyType,
                KeyFactory<? super K, ? extends IK> keys,
                EventApplier<? super IK, ? super E, ? super C> applier) {
            this.keyType = keyType;
            this.keys = keys;
            this.applier = applier;
        }

        public StoreSubscribableBuilder<K, IK, C, E> storingSubscribers(int targetSize, MapFactory type, Supplier<Set<C>> setFactory) {
            SubscribersStoreImpl<IK, C> store = new SubscribersStoreImpl<>(targetSize, type, setFactory);
            return new StoreSubscribableBuilder<>(keyType, keys, applier, store);
        }

        public SubscriberStorageBuilder<K, IK, E, C> storingSubscribersIn(SetFactory<Object> supp) {
            return new SubscriberStorageBuilder<>(this).withSets(supp);
        }

        public SubscriberStorageBuilder<K, IK, E, C> storingSubscribersIn(SetFactories setTypes) {
            return new SubscriberStorageBuilder<>(this).withSets(setTypes);
        }

        public SubscriberStorageBuilder<K, IK, E, C> withInitialMapSize(int mapSize) {
            return new SubscriberStorageBuilder<>(this).withInitialMapSize(mapSize);
        }

        public SubscriberStorageBuilder<K, IK, E, C> withInitialSubscriberSetSize(int setSize) {
            return new SubscriberStorageBuilder<>(this).withInitialSubscriberSetSize(setSize);
        }

        public SubscriberStorageBuilder<K, IK, E, C> withCacheTypes(MapFactory types) {
            return new SubscriberStorageBuilder<>(this).withCacheType(types);
        }
    }

    public static final class SubscriberStorageBuilder<K, IK, E, C> {

        private SetFactory<Object> sets;
        private int initialSubscriberSetSize = 16;
        private boolean threadSafe = true;
        private final EventSubscribableBuilder<K, IK, E, C> outer;
        private MapFactory mappingType = MapFactories.WEAK;
        private int initialKeyMapSize = 16;

        public SubscriberStorageBuilder(EventSubscribableBuilder<K, IK, E, C> outer) {
            this.outer = outer;
        }

        public SubscriberStorageBuilder<K, IK, E, C> withSets(SetFactories setTypes) {
            sets = setTypes;
            return this;
        }

        public SubscriberStorageBuilder<K, IK, E, C> withSets(SetFactory<Object> setTypes) {
            this.sets = setTypes;
            return this;
        }

        public SubscriberStorageBuilder<K, IK, E, C> withInitialMapSize(int initialSize) {
            this.initialKeyMapSize = Checks.greaterThanZero("initialSize", initialSize);
            return this;
        }

        public SubscriberStorageBuilder<K, IK, E, C> withInitialSubscriberSetSize(int initialSize) {
            this.initialSubscriberSetSize = Checks.greaterThanZero("initialSize", initialSize);
            return this;
        }

        public SubscriberStorageBuilder<K, IK, E, C> withCacheType(MapFactory type) {
            mappingType = type;
            return this;
        }

        public StoreSubscribableBuilder<K, IK, C, E> threadSafe() {
            return outer.storingSubscribers(initialKeyMapSize, mappingType,
                    sets.setSupplier(initialSubscriberSetSize, threadSafe));
        }
    }

    public static final class StoreSubscribableBuilder<K, IK, C, E> {

        private final Class<K> keyType;
        private final KeyFactory<? super K, ? extends IK> keys;
        private final SubscribersStoreImpl<IK, C> store;
        private final DelegatingNotifier<K, E> del = new DelegatingNotifier<>();
        private final EventApplier<? super IK, ? super E, ? super C> applier;

        StoreSubscribableBuilder(
                Class<K> keyType,
                KeyFactory<? super K, ? extends IK> keys,
                EventApplier<? super IK, ? super E, ? super C> applier,
                SubscribersStoreImpl<IK, C> store) {
            this.keys = keys;
            this.keyType = keyType;
            this.applier = applier;
            this.store = store;
        }

        public FinishableSubscribableBuilder<K, IK, E, C> withAsynchronousEventDelivery(Executor executor) {
            return new FinishableSubscribableBuilder<>(keyType, keys, applier, store, del, del.async(executor));
        }

        public FinishableSubscribableBuilder<K, IK, E, C> withAsynchronousEventDelivery() {
            return new FinishableSubscribableBuilder<>(keyType, keys, applier, store, del, del.async(ForkJoinPool.commonPool()));
        }

        public FinishableSubscribableBuilder<K, IK, E, C> withEventDelivery(Function<SubscribableNotifier<K, E>, SubscribableNotifier<? super K, ? super E>> xform) {
            return new FinishableSubscribableBuilder<>(keyType, keys, applier, store, del, xform.apply(del));
        }

        public FinishableSubscribableBuilder<K, IK, E, C> withSynchronousEventDelivery() {
            return new FinishableSubscribableBuilder<>(keyType, keys, applier, store, del, del);
        }

        public FinishableSubscribableBuilder<K, IK, E, C> withCoalescedAsynchronousEventDelivery(ScheduledExecutorService threadPool, int delay, TimeUnit delayUnits) {
            return withCoalescedAsynchronousEventDelivery(threadPool, MapFactories.WEAK, delay, delayUnits);
        }

        public FinishableSubscribableBuilder<K, IK, E, C> withCoalescedAsynchronousEventDelivery(int delay, TimeUnit delayUnits) {
            return withCoalescedAsynchronousEventDelivery(Executors.newScheduledThreadPool(3), MapFactories.WEAK, delay, delayUnits);
        }

        public FinishableSubscribableBuilder<K, IK, E, C> withCoalescedAsynchronousEventDelivery(ScheduledExecutorService threadPool, MapFactories cacheType, int delay, TimeUnit delayUnits) {
            return new FinishableSubscribableBuilder<>(keyType, keys, applier, store, del, del.coalescing(threadPool, cacheType, Checks.greaterThanOne("delay", delay), delayUnits));
        }

        public FinishableSubscribableBuilder<K, IK, E, C> withCoalescedAsynchronousEventDelivery(BooleanSupplier coalesceWhenTrue, ScheduledExecutorService threadPool, MapFactories cacheType, int delay, TimeUnit delayUnits) {
            return new FinishableSubscribableBuilder<>(keyType, keys, applier, store, del, del.coalescing(coalesceWhenTrue, threadPool, cacheType, Checks.greaterThanOne("delay", delay), delayUnits));
        }
    }

    public static final class FinishableSubscribableBuilder<K, IK, E, C> {

        private final Class<K> keyType;
        private final KeyFactory<? super K, ? extends IK> keys;
        private final EventApplier<? super IK, ? super E, ? super C> applier;
        private final SubscribersStoreImpl<IK, C> store;
        private final DelegatingNotifier<K, E> del;
        private final SubscribableNotifier<? super K, ? super E> subscribeNotifier;
        private TriConsumer<? super K, ? super IK, ? super C> onSubscribe;
        private TriConsumer<? super K, ? super IK, ? super C> onUnsubscribe;

        FinishableSubscribableBuilder(Class<K> keyType, KeyFactory<? super K, ? extends IK> keys, EventApplier<? super IK, ? super E, ? super C> applier, SubscribersStoreImpl<IK, C> store, DelegatingNotifier<K, E> del, SubscribableNotifier<? super K, ? super E> externalNotifier) {
            this.keyType = keyType;
            this.keys = keys;
            this.applier = applier;
            this.store = store;
            this.del = del;
            this.subscribeNotifier = externalNotifier;
        }

        public FinishableSubscribableBuilder<K, IK, E, C> onSubscribe(TriConsumer<? super K, ? super IK, ? super C> onSubscribe) {
            if (this.onSubscribe == null) {
                this.onSubscribe = onSubscribe;
            } else {
                TriConsumer<? super K, ? super IK, ? super C> old = this.onSubscribe;
                this.onSubscribe = (k, ik, c) -> {
                    old.accept(k, ik, c);
                    onSubscribe.accept(k, ik, c);
                };
            }
            return this;
        }

        public FinishableSubscribableBuilder<K, IK, E, C> onUnsubscribe(TriConsumer<? super K, ? super IK, ? super C> onSubscribe) {
            if (this.onUnsubscribe == null) {
                this.onUnsubscribe = onSubscribe;
            } else {
                TriConsumer<? super K, ? super IK, ? super C> old = this.onUnsubscribe;
                this.onUnsubscribe = (k, ik, c) -> {
                    old.accept(k, ik, c);
                    onSubscribe.accept(k, ik, c);
                };
            }
            return this;
        }

        public SubscribableContents<K, IK, C, E> build() {
            SubscribableImpl<K, IK, C, E> result = new SubscribableImpl<>(keys, applier, store, store.mutator(), onSubscribe, del, onUnsubscribe);
            SubscribersStoreController<K, C> mut = store.mutator().converted(keys);
            SubscribableContents<K, IK, C, E> contents
                    = new SubscribableContents<>(result, subscribeNotifier, mut, store.mutator(), store);
            return contents;
        }
    }

    public static final class SubscribableContents<K, IK, C, E> {

        public final Subscribable<K, C> subscribable;
        public final SubscribableNotifier<? super K, ? super E> eventInput;
        public final SubscribersStoreController<K, C> subscribersManager;
        public final SubscribersStore<IK, C> store;
        public final CacheMaintainer<K> caches;

        @SuppressWarnings("unchecked")
        SubscribableContents(Subscribable<K, C> subscribable,
                SubscribableNotifier<? super K, ? super E> eventInput,
                SubscribersStoreController<K, C> subscribersManager,
                SubscribersStoreController<IK, C> rawubscribersManager,
                SubscribersStore<IK, C> store) {
            this.subscribable = subscribable;
            caches = (SubscribableImpl<K, IK, C, E>) subscribable;
            this.eventInput = eventInput;
            this.subscribersManager = subscribersManager;
            this.store = store;
        }
    }

    private SubscribableBuilder() {
        throw new AssertionError();
    }
}
