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
package com.mastfrog.util.thread;

import static com.mastfrog.util.Checks.notNull;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A non-blocking, thread-safe, memory-efficient queue using a simple linked
 * list structure and atomic add and drain operations.
 * <p>
 * Note that iteration occurs in reverse order.
 *
 * @author Tim Boudreau
 */
public final class AtomicLinkedQueue<Message> implements Iterable<Message> {

    // A basic linked list structure, where the head is found by
    // iterating backwards
    private AtomicReference<MessageEntry<Message>> tail;
    private Runnable onAdd;

    /**
     * Create the a queue and add the first element.
     *
     * @param message The first element
     */
    public AtomicLinkedQueue(Message message) {
        tail = new AtomicReference<>(new MessageEntry<>(null, notNull("message", message)));
    }

    public AtomicLinkedQueue(Iterable<Message> it) {
        tail = new AtomicReference<>();
        for (Message m : notNull("it", it)) {
            add(m);
        }
    }

    public AtomicLinkedQueue(AtomicLinkedQueue<Message> q) {
        tail = notNull("q", q).tail;
    }

    /**
     * Create a new queue.
     */
    public AtomicLinkedQueue() {
        tail = new AtomicReference<>();
    }

    public AtomicLinkedQueue<Message> copy() {
        return new AtomicLinkedQueue<>(this);
    }

    public void reverseInPlace() {
        tail.updateAndGet((MessageEntry<Message> t) -> {
            if (t == null) {
                return t;
            }
            MessageEntry<Message> head = new MessageEntry<>(null, t.message);
            t = t.prev;
            while (t != null) {
                head = new MessageEntry<>(head, t.message);
                t = t.prev;
            }
            return head;
        });
    }

    /**
     * Add an element to the tail of the queue
     *
     * @param message
     * @return this
     */
    public AtomicLinkedQueue<Message> add(final Message message) {
        tail.getAndUpdate(new Applier<>(notNull("message", message)));
        if (onAdd != null) {
            onAdd.run();
        }
        return this;
    }

    /**
     * Provide a runnable to invoke on every add. <i><b>Important:</b> - this is
     * a mechanism for you to <b>schedule</b> something to be done with the
     * queue - schedule a job or notify a backgroun thread. Do not do real work
     * here, and especially do not call anything that synchronizes or
     * blocks.</i> The idea here is simply to provide a more flexible mechanism
     * than a thread latch or similar can offer so as not to assume that waking
     * up a thread is the only possible thing one might want.
     *
     * @param run A runnable
     * @return this
     */
    public AtomicLinkedQueue onAdd(Runnable run) {
        this.onAdd = run;
        return this;
    }

    /**
     * Convenience method to trigger a OneThreadLatch on add.
     *
     * @param latch The latch
     * @return this
     */
    public AtomicLinkedQueue onAdd(OneThreadLatch latch) {
        this.onAdd = latch::releaseOne;
        return this;
    }

    private static class Applier<Message> implements UnaryOperator<MessageEntry<Message>> {

        private final Message message;

        public Applier(Message message) {
            this.message = message;
        }

        @Override
        public MessageEntry<Message> apply(MessageEntry<Message> t) {
            if (t == null) {
                return new MessageEntry<>(null, message);
            } else {
                return new MessageEntry<>(t, message);
            }
        }
    }

    /**
     * Drain the queue, returning a list
     *
     * @return A list of messages
     */
    public List<Message> drain() {
        MessageEntry<Message> oldTail = tail.getAndSet(null);
        // Populate the list iterating backwards from the tail
        if (oldTail != null) {
            if (oldTail.prev == null) {
                return Collections.<Message>singletonList(oldTail.message);
            }
            List<Message> all = new LinkedList<>();
            oldTail.drainTo(all);
            return all;
        }
        return Collections.emptyList();
    }

    /**
     * Drain the queue to an existing list.  Note that the queue
     * will be drained to index 0 in the list - if it has existing
     * contents, those will be pushed forward.
     *
     * @param all The list
     * @return the list
     */
    public List<Message> drainTo(List<Message> all) {
        MessageEntry<Message> oldTail = tail.getAndSet(null);
        // Populate the list iterating backwards from the tail
        if (oldTail != null) {
            if (oldTail.prev == null) {
                all.add(oldTail.message);
                return all;
            }
            oldTail.drainTo(all);
        }
        return all;
    }

    /**
     * Drain the queue, passing a visitor to visit each element (in reverse
     * order) as it is drained.
     *
     * @param visitor A visitor
     * @return A list of elements
     */
    public List<Message> drain(Consumer<Message> visitor) {
        // Do the minimal amount under the lock
        MessageEntry<Message> oldTail = tail.getAndSet(null);
        // Populate the list iterating backwards from the tail
        if (oldTail != null) {
            if (oldTail.prev == null) {
                visitor.accept(oldTail.message);
                return Collections.<Message>singletonList(oldTail.message);
            }
            List<Message> all = new LinkedList<>();
            oldTail.drainTo(all, visitor);
            return all;
        }
        return Collections.emptyList();
    }

    /**
     * Returns an iterator in <i>reverse order</i> of the queue contents at the
     * time the iterator was created.
     *
     * @return An iterator
     */
    public Iterator<Message> iterator() {
        MessageEntry<Message> tail = this.tail.get();
        if (tail == null) {
            return Collections.emptyIterator();
        }
        return new It<Message>(tail);
    }

    public List<Message> asList() {
        List<Message> result = new LinkedList<>();
        MessageEntry<Message> t = tail.get();
        while (t != null) {
            result.add(0, t.message);
            t = t.prev;
        }
        return result;
    }

    public void clear() {
        tail.set(null);
    }

    /**
     * Divide the contents of this queue into two others based on matching the
     * passed predicate, and clear this queue.
     *
     * @param pred A predicate
     * @param c A consumer which will be called with two queues, accepted and
     * rejected in that argument order
     */
    public void filterAndDrain(Predicate<Message> pred, BiConsumer<AtomicLinkedQueue<Message>, AtomicLinkedQueue<Message>> c) {
        filter(tail.getAndSet(null), pred, c);
    }

    /**
     * Divide the contents of this queue into two others based on matching the
     * passed predicate, not modifying.
     *
     * @param pred A predicate
     * @param c A consumer which will be called with two queues, accepted and
     * rejected in that argument order
     */
    public void filter(Predicate<Message> pred, BiConsumer<AtomicLinkedQueue<Message>, AtomicLinkedQueue<Message>> c) {
        filter(tail.get(), pred, c);
    }

    private void filter(MessageEntry<Message> t, Predicate<Message> pred, BiConsumer<AtomicLinkedQueue<Message>, AtomicLinkedQueue<Message>> c) {
        AtomicLinkedQueue<Message> accepted = new AtomicLinkedQueue<>();
        AtomicLinkedQueue<Message> rejected = new AtomicLinkedQueue<>();
        while (t != null) {
            if (pred.test(t.message)) {
                accepted.add(t.message);
            } else {
                rejected.add(t.message);
            }
            t = t.prev;
        }
        accepted.reverseInPlace();
        rejected.reverseInPlace();
        c.accept(accepted, rejected);
    }

    static class It<T> implements Iterator<T> {

        private MessageEntry<T> en;

        public It(MessageEntry<T> en) {
            this.en = en;
        }

        @Override
        public boolean hasNext() {
            return en != null;
        }

        @Override
        public T next() {
            T result = en.message;
            en = en.prev;
            return result;
        }
    }

    /**
     * Determine if the queue is empty.
     *
     * @return true if it is empty
     */
    public boolean isEmpty() {
        return tail.get() == null;
    }

    /**
     * Get the current size
     *
     * @return The size
     */
    public int size() {
        MessageEntry<Message> tail = this.tail.get();
        int result = 0;
        while (tail != null) {
            result++;
            tail = tail.prev;
        }
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        MessageEntry<Message> tail = this.tail.get();
        while (tail != null) {
            sb.insert(0, tail.message);
            if (tail.prev != null) {
                sb.insert(0, ",");
            }
            tail = tail.prev;
        }
        return sb.toString();
    }

    /**
     * A single linked list entry in the queue
     */
    private static final class MessageEntry<Message> {

        private MessageEntry<Message> prev;
        private Message message;

        MessageEntry(MessageEntry<Message> prev, Message message) {
            this.prev = prev;
            this.message = message;
        }

        void drainTo(List<? super Message> messages) {
            // Iterate backwards populating the list of messages,
            // and also collect the total byte count
            MessageEntry<Message> e = this;
            while (e != null) {
                messages.add(0, e.message);
                e = e.prev;
            }
        }

        void drainTo(List<? super Message> messages, Consumer<Message> visitor) {
            // Iterate backwards populating the list of messages,
            // and also collect the total byte count
            MessageEntry<Message> e = this;
            while (e != null) {
                visitor.accept(e.message);;
                messages.add(0, e.message);
                e = e.prev;
            }
        }
    }
}
