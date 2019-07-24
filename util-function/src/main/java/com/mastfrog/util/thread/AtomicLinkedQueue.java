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

import com.mastfrog.util.preconditions.Checks;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A non-blocking, thread-safe, memory-efficient queue using a simple linked
 * list structure and atomic add and drain operations. Atomicity is achieved by
 * using a singly-tail-linked data structure using atomic references. Mutation
 * operations that affect the tail, such as <code>add()</code> and
 * <code>pop()</code> are guaranteed to be thread-safe and non-blocking;
 * operations that affect other parts of the queue are not (though
 * <code>removeByIdentity()</code> will tell you if it failed).
 * <p>
 * Note that iteration occurs in reverse order. Identity-based removal
 * operations exist; under concurrency they may spuriously fail, but will report
 * that to the caller with their result value, and the caller may retry.
 *
 * @author Tim Boudreau
 */
public final class AtomicLinkedQueue<Message> implements Iterable<Message> {

    // A basic linked list structure, where the head is found by
    // iterating backwards
    private final AtomicReference<MessageEntry<Message>> tail;
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
        tail = copyRef(q.tail);
    }

    private static <Message> AtomicReference<MessageEntry<Message>> copyRef(AtomicReference<MessageEntry<Message>> ref) {
        MessageEntry<Message> orig = ref.get();
        if (orig == null) {
            return new AtomicReference<>();
        } else {
            return new AtomicReference<>(orig.copy());
        }
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

    /**
     * Reverse the contents of this queue in-place. This method is not
     * guaranteed to be unaffected by operations that modify the queue in other
     * threads.
     */
    public void reverseInPlace() {
        tail.updateAndGet((MessageEntry<Message> t) -> {
            if (t == null) {
                return t;
            }
            MessageEntry<Message> head = new MessageEntry<>(null, t.message);
            t = t.getPrev();
            while (t != null) {
                head = new MessageEntry<>(head, t.message);
                t = t.getPrev();
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
     * Drain the queue, returning a list, tail-first.
     *
     * @return A list of messages
     */
    public List<Message> drain() {
        MessageEntry<Message> oldTail = tail.getAndSet(null);
        // Populate the list iterating backwards from the tail
        if (oldTail != null) {
            if (oldTail.getPrev() == null) {
                return Collections.<Message>singletonList(oldTail.message);
            }
            List<Message> all = new LinkedList<>();
            oldTail.drainTo(all);
            return all;
        }
        return Collections.emptyList();
    }

    /**
     * Drain the queue to an existing list. Note that the queue will be drained
     * to index 0 in the list - if it has existing contents, those will be
     * pushed forward (for best performance, pass in a LinkedList).
     *
     * @param all The list
     * @return the list
     */
    public List<Message> drainTo(List<Message> all) {
        MessageEntry<Message> oldTail = tail.getAndSet(null);
        // Populate the list iterating backwards from the tail
        if (oldTail != null) {
            if (oldTail.getPrev() == null) {
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
            if (oldTail.getPrev() == null) {
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
    @Override
    public Iterator<Message> iterator() {
        MessageEntry<Message> tailLocal = this.tail.get();
        if (tailLocal == null) {
            return Collections.emptyIterator();
        }
        return new It<>(tailLocal);
    }

    /**
     * Return a list containing the contents of this queue in order.
     *
     * @return A list
     */
    public List<Message> asList() {
        List<Message> result = new LinkedList<>();
        MessageEntry<Message> t = tail.get();
        while (t != null) {
            result.add(0, t.message);
            t = t.getPrev();
        }
        return result;
    }

    /**
     * Clear the contents of this queue.
     */
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
            t = t.getPrev();
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
            en = en.getPrev();
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
        MessageEntry<Message> tailLocal = this.tail.get();
        int result = 0;
        while (tailLocal != null) {
            result++;
            tailLocal = tailLocal.getPrev();
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        MessageEntry<Message> tailLocal = this.tail.get();
        while (tailLocal != null) {
            sb.insert(0, tailLocal.message);
            if (tailLocal.getPrev() != null) {
                sb.insert(0, ",");
            }
            tailLocal = tailLocal.getPrev();
        }
        return sb.toString();
    }

    /**
     * Pop the most recently added element from this queue.
     *
     * @return A message or nulk
     */
    @SuppressWarnings("unchecked")
    public Message pop() {
        Object[] m = new Object[1];
        tail.getAndUpdate(msg -> {
            if (msg == null) {
                return null;
            }
            m[0] = msg.message;
            return msg.getPrev();
        });
        return (Message) m[0];
    }

    /**
     * Pop the tail of this queue and push it into another queue.
     *
     * @param other The other queue
     * @param ifNull Will supply a value if this queue is empty
     * @return The maessage which was moved
     */
    @SuppressWarnings("unchecked")
    public Message popInto(AtomicLinkedQueue<Message> other, Supplier<Message> ifNull) {
        Checks.notSame("this", "other", this, other);
        Object[] result = new Object[1];
        tail.getAndUpdate(oldTail -> {
            if (oldTail != null) {
                MessageEntry<Message> prev = oldTail.getPrev();
                other.tail.getAndUpdate(oldOtherTail -> {
                    oldTail.updatePrev(oldPrev -> {
                        result[0] = oldTail.message;
                        return oldOtherTail;
                    });
                    result[0] = oldTail.message;
                    return oldTail;
                });
                return prev;
            } else {
                other.tail.getAndUpdate(otherOldTail -> {
                    Message msg;
                    result[0] = msg = ifNull.get();
                    return new MessageEntry<>(otherOldTail, msg);
                });
                return null;
            }
        });
        return (Message) result[0];
    }

    /**
     * Pop the tail of this queue, removing it, and using the passed supplier if
     * this queue was empty.
     *
     * @param ifNone A supplier for values when none is available to pop
     * @return The popped message or the result from the supplier
     */
    public Message pop(Supplier<Message> ifNone) {
        Message result = pop();
        return result == null ? ifNone.get() : result;
    }

    /**
     * Remove an object, using identity (not equality) testing.
     * <i><b>Behavior under concurrent access:</b> This method may return false
     * because another thread is part-way through removing a node adjacent to
     * one that exists and does contain the requested object - making it
     * apparently not present when the linked list of nodes is traversed. So
     * removal <u>may</u>
     * spuriously fail, but the return value will reliably indicate that.</i>
     *
     * @param msg An element to remove
     * @return True if it is removed
     */
    public boolean removeByIdentity(Message msg) {
        Remover<Message> remover = new Remover<>(msg);
        tail.updateAndGet(remover);
        if (!remover.removed) {
            tail.updateAndGet(remover);
        }
        return remover.removed;
    }

    boolean contains(Message msg) {
        MessageEntry<Message> top = tail.get();
        while (top != null) {
            if (msg == top.message) {
                return true;
            }
            top = top.getPrev();
        }
        return false;
    }

    static final class Remover<Message> implements UnaryOperator<MessageEntry<Message>> {

        private final Message toRemove;
        boolean removed;

        public Remover(Message toRemove) {
            this.toRemove = toRemove;
        }

        @Override
        public MessageEntry<Message> apply(MessageEntry<Message> t) {
            if (t == null) {
                return null;
            }
            MessageEntry<Message> prev = t.prev;
            if (t.message == toRemove) {
                removed = true;
                return prev;
            } else {
                t.updatePrev(this);
            }
            return t;
        }
    }

    /**
     * A single linked list entry in the queue
     */
    static final class MessageEntry<Message> {

        private volatile MessageEntry<Message> prev;
        final Message message;
        @SuppressWarnings("rawtype")
        private static final AtomicReferenceFieldUpdater UPDATER
                = AtomicReferenceFieldUpdater.newUpdater(MessageEntry.class, MessageEntry.class, "prev");

        MessageEntry(MessageEntry<Message> prev, Message message) {
            this.prev = prev;
            this.message = message;
        }

        @Override
        public String toString() {
            return Objects.toString(message);
        }

        @SuppressWarnings("unchecked")
        MessageEntry<Message> getPrev() {
            return (MessageEntry<Message>) UPDATER.get(this);
        }

        MessageEntry<Message> copy() {
            return new MessageEntry<>(prev == null ? null : prev.copy(), message);
        }

        @SuppressWarnings("unchecked")
        void setPrev(MessageEntry<Message> prev) {
            UPDATER.set(this, prev);
        }

        @SuppressWarnings("unchecked")
        void updatePrev(UnaryOperator<MessageEntry<Message>> uo) {
            UPDATER.updateAndGet(this, uo);
        }

        @SuppressWarnings("unchecked")
        boolean updatePrev(MessageEntry<Message> expect, MessageEntry<Message> nue) {
            return UPDATER.compareAndSet(this, expect, nue);
        }

        void drainTo(List<? super Message> messages) {
            // Iterate backwards populating the list of messages,
            // and also collect the total byte count
            MessageEntry<Message> e = this;
            while (e != null) {
                messages.add(0, e.message);
                e = e.getPrev();
            }
        }

        void drainTo(List<? super Message> messages, Consumer<Message> visitor) {
            // Iterate backwards populating the list of messages,
            // and also collect the total byte count
            MessageEntry<Message> e = this;
            while (e != null) {
                visitor.accept(e.message);
                messages.add(0, e.message);
                e = e.getPrev();
            }
        }
    }
}
