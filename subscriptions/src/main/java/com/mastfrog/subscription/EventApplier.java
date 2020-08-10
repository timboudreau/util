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

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Applies an subscribable event to a set of subscribers - a subscriber may not
 * be a simple Consumer&lt;E&gt; - it may need to be passed the key as well and
 * be a BiConsumer, or have the event translated to different types it
 * understands - this interface provides the indirection needed to be flexible
 * on the concept of what "consuming an event" means, so the clients can be
 * written intuitively.
 *
 * @author Tim Boudreau
 */
public interface EventApplier<K, E, C> {

    void apply(K key, E event, Collection<? extends C> consumers);

    default EventApplier<K, E, C> filter(BiPredicate<K, E> tester) {
        return (k, e, consumers) -> {
            if (tester.test(k, e)) {
                EventApplier.this.apply(k, e, consumers);
            }
        };
    }

    static <K, E> EventApplier<K, E, Consumer<? super E>> consumers() {
        return (k, e, consumers) -> {
            for (Consumer<? super E> c : consumers) {
                c.accept(e);
            }
        };
    }

    static <K, E> EventApplier<K, E, BiConsumer<? super K, ? super E>> biconsumers() {
        return (k, e, consumers) -> {
            for (BiConsumer<? super K, ? super E> c : consumers) {
                c.accept(k, e);
            }
        };
    }

    static <K, E> EventApplier<K, E, BiConsumer<? super K, ? super E>> biconsumers(Predicate<? super Throwable> onError) {
        return (k, e, consumers) -> {
            for (BiConsumer<? super K, ? super E> c : consumers) {
                try {
                    c.accept(k, e);
                } catch (Exception | Error err) {
                    if (onError.test(err)) {
                        break;
                    }
                }
            }
        };
    }

    static <K, E> EventApplier<K, E, Consumer<? super E>> consumers(Predicate<? super Throwable> onError) {
        return (k, e, consumers) -> {
            for (Consumer<? super E> c : consumers) {
                try {
                    c.accept(e);
                } catch (Exception | Error err) {
                    if (onError.test(err)) {
                        break;
                    }
                }
            }
        };
    }
}
