package com.mastfrog.subscription;

import com.mastfrog.function.TriConsumer;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.Collection;

/**
 *
 * @author Tim Boudreau
 */
class SubscribableImpl<K, IK, C, E> extends AbstractCacheMaintainer<K> implements Subscribable<K, C>, SubscribableNotifier<K, E> {

    private final KeyFactory<? super K, ? extends IK> keys;

    private final EventApplier<? super IK, ? super E, ? super C> applier;
    private final SubscribersStore<IK, C> store;
    private final SubscribersStoreController<IK, C> storeModifier;
    private final TriConsumer<? super K, ? super IK, ? super C> onSubscribe;
    private final TriConsumer<? super K, ? super IK, ? super C> onUnsubscribe;

    @SuppressWarnings("LeakingThisInConstructor")
    public SubscribableImpl(KeyFactory<? super K, ? extends IK> keys,
            EventApplier<? super IK, ? super E, ? super C> applier,
            SubscribersStore<IK, C> store,
            SubscribersStoreController<IK, C> storeModifier,
            TriConsumer<? super K, ? super IK, ? super C> onSubscribe,
            DelegatingNotifier<K, E> delegatee,
            TriConsumer<? super K, ? super IK, ? super C> onUnsubscribe) {
        this.keys = notNull("keys", keys);
        this.applier = notNull("applier", applier);
        this.store = notNull("store", store);
        this.storeModifier = notNull("storeModifier", storeModifier);
        this.onSubscribe = onSubscribe;
        this.onUnsubscribe = onUnsubscribe;
        delegatee.setDelegate(this);
    }

    @Override
    public void subscribe(K key, C consumer) {
        IK internalKey = keys.constructKey(key);
        if (internalKey != null) {
            if (storeModifier.add(internalKey, consumer) && onSubscribe != null) {
                onSubscribe.accept(key, internalKey, consumer);
            }
        }
    }

    @Override
    public void unsubscribe(K type, C consumer) {
        IK internalKey = keys.constructKey(type);
        if (internalKey != null) {
            if (storeModifier.remove(internalKey, consumer) && onUnsubscribe != null) {
                onUnsubscribe.accept(type, internalKey, consumer);
            }
        }
    }

    void internalOnEvent(IK internalKey, E event) {
        Collection<? extends C> consumers = store.subscribersTo(internalKey);
        if (!consumers.isEmpty()) {
            applier.apply(internalKey, event, consumers);
        }
    }

    @Override
    public void onEvent(K key, E event) {
        IK internalKey = keys.constructKey(key);
        internalOnEvent(internalKey, event);
    }

    @Override
    public void destroyed(K type) {
        storeModifier.removeAll(keys.constructKey(type));
    }
}
