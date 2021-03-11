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
package com.mastfrog.util.collections;

import com.mastfrog.util.preconditions.Checks;
import static com.mastfrog.util.preconditions.Checks.notNull;
import com.sun.org.apache.xpath.internal.operations.Bool;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A non-blocking, thread-safe, memory-efficient FIFO queue using a simple
 * linked list structure and atomic add and drain operations. Atomicity is
 * achieved by using a singly-tail-linked data structure using atomic
 * references. Mutation operations that affect the tail, such as
 * <code>add()</code> and <code>pop()</code> are guaranteed to be thread-safe
 * and non-blocking; operations that affect other parts of the queue are not
 * (though <code>removeByIdentity()</code> will tell you if it failed).
 * <p>
 * Note that iteration occurs in reverse order. Identity-based removal
 * operations exist; under concurrency they may spuriously fail, but will report
 * that to the caller with their result value, and the caller may retry.
 * </p>
 * Typical usage is to collect items that are applied to some work which will be
 * scheduled in the future - a rough sketch
 * <pre>
 *
 * class WorkDoer implements Runnable {
 *
 * private final AtomicLinkedQueue&lt;Listener&gt; listeners = new AtomicLinkedQueue&lt;&gt;();
 * private final ScheduledExecutorService svc = Executors.newScheduledThreadPool(5);
 * private final AtomicBoolean scheduled = new AtomicBoolean();
 *
 * void listenToWork(Listener listener) {
 *     listeners.add(listener);
 *     if (!scheduled.compareAndSet(false, true)) {
 *        svc.schedule(this, 200, TimeUnit.MILLISECONDS);
 *     }
 * }
 *
 * public void run() {
 *     try {
 *       List&lt;Listener&gt; all = new ArrayList&lt;&gt;();
 *       // Since we are atomic, not synchronized, we need to
 *       // loop to guarantee we catch any items added after we enter here
 *       do {
 *         all.clear();
 *         listeners.drainTo(all); // listeners is now empty
 *         all.forEach(listener -> listener.doSomething());
 *       } (while !all.isEmpty());
 *     } finally {
 *        scheduled.set(false);
 *     }
 * }
 * }
 * </pre>
 * <i>This class originally appeared in <code>com.mastfrog.util.thread</code>;
 * the version there will not be further maintained.</i>. Note: Remove
 * operations other than <code>pop()</code> are expensive, particularly under
 * concurrent access.
 *
 * @author Tim Boudreau
 */
public final class AtomicLinkedQueue<Message> implements Iterable<Message>, Queue<Message> {

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

    public void swapContents(AtomicLinkedQueue<Message> other) {
        tail.getAndUpdate(t -> {
            return other.tail.getAndUpdate(t2 -> {
                return t;
            });
        });
    }

    /**
     * The inverse of <code>drainTo()</code> take the contents of another queue
     * and add it to this one.
     *
     * @param other
     */
    public void transferContentsFrom(AtomicLinkedQueue<Message> other) {
        if (other == this) {
            return;
        }
        MessageEntry<Message> otherHead = other.detachHead();
        if (otherHead != null) {
            // Under concurrency, this could be mutated by another thread that
            // is already re-stitching the linked list, so make a private
            // copy - was running into an NPE because otherHead.getPrev() was
            // non-null in the whiie() clause but has been nulled by the
            // time we loop back around
            otherHead = otherHead.copy();
            MessageEntry<Message> myOldTail = tail.getAndSet(otherHead);
            // This does open a small race-window.
            do {
                MessageEntry<Message> next = otherHead.getPrev();
                if (next == null) {
                    break;
                }
                otherHead = next;
            } while (otherHead.getPrev() != null);
            otherHead.setPrev(myOldTail);
        }
    }

    /**
     * Drain this <code>AtomicLinkedQueue</code> into another one; note this
     * overloaded method is <i>far</i> more efficient than any of the other
     * <code>drainTo()</code> methods, since it simply removes the tail of this
     * queue and attaches it to the passed one, with no iteration required.
     *
     * @param other Another queue
     */
    public void drainTo(AtomicLinkedQueue<Message> other) {
        if (other == this) {
            return;
        }
        MessageEntry<Message> oldTail = tail.getAndSet(null);
        if (oldTail == null) {
            return;
        }
        MessageEntry<Message> oldHead = oldTail;
        while (oldHead.getPrev() != null) {
            oldHead = oldHead.getPrev();
        }
        MessageEntry<Message> fh = oldHead;
        MessageEntry otherTail = other.tail.getAndUpdate(old -> {
            if (old != null) {
                fh.setPrev(old);
            }
            return oldTail;
        });
    }

    private MessageEntry<Message> detachHead() {
        return tail.getAndSet(null);
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
     * For API-compatibility with Queue - same as <code>add(message)</code>.
     *
     * @param message
     */
    public void push(final Message message) {
        add(message);
    }

    /**
     * Add an element to the tail of the queue
     *
     * @param message
     * @return this
     */
    @Override
    public boolean add(final Message message) {
        tail.getAndUpdate(new Applier<>(notNull("message", message)));
        if (onAdd != null) {
            onAdd.run();
        }
        return true;
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
    public AtomicLinkedQueue<Message> onAdd(Runnable run) {
        this.onAdd = run;
        return this;
    }

    private static class Applier<Message> implements UnaryOperator<MessageEntry<Message>> {

        private final Message message;

        Applier(Message message) {
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
    @Override
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

        It(MessageEntry<T> en) {
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
    @Override
    public boolean isEmpty() {
        return tail.get() == null;
    }

    /**
     * Get the current size
     *
     * @return The size
     */
    @Override
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
     * Get the message at the specified index; note that since this is a
     * singly-linked atomic queue, this is not a constant-time operation, and
     * the queue's contents may be changed by another thread while it is
     * proceeding; in particular, do not use if you expect the queue to get
     * <i>smaller</i> while in a call to get().
     *
     * @param index The offset from the tail
     * @throws NoSuchElementException if the index > size()
     * @throws IllegalArgumentException if the index is negative
     * @return A message
     */
    public Message get(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index");
        }
        int count = 0;
        MessageEntry<Message> t = tail.get();
        while (count < index) {
            t = t.prev;
            count++;
            if (t == null) {
                throw new NoSuchElementException("No element " + index);
            }
        }
        return t.message;
    }

    /**
     * Atomically replace the contents of this queue with the passed collection;
     * note that this will be done in <i>reverse-iteration-order</i>, so the top
     * message will be the <i>last</i> one in the passed collection.
     *
     * @param newContents The new contents
     * @throws IllegalArgumentException if a null is encountered in the passed
     * collection
     * @return this
     */
    public AtomicLinkedQueue<Message> replaceContents(Iterable<? extends Message> newContents) {
        MessageEntry<Message> curr = null;
        for (Message m : notNull("newContents", newContents)) {
            if (m == null) {
                throw new IllegalArgumentException("newContents collection contains nulls");
            }
            curr = new MessageEntry<>(curr, m);
        }
        tail.set(curr);
        return this;
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
     * Remove an object, using identity (not equality) testing. Note this method
     * may loop repeatedly creating a copy of the contents of this queue in
     * order to guarantee atomicity. If the object occurs more than once in the
     * queue, the first occurrence is removed.
     *
     * @param msg An element to remove
     * @return True if it is removed
     */
    public synchronized boolean removeByIdentity(Object msg) {
        for (;;) {
            MessageEntry<Message> t = tail.get();
            if (t == null) {
                return false;
            }
            boolean[] found = new boolean[1];
            MessageEntry<Message> copy = t.deepCopyRemoving(msg, found);
            if (!found[0]) {
                return false;
            }
            if (tail.compareAndSet(t, copy)) {
                return true;
            }
        }
    }

    @Override
    public boolean contains(Object msg) {
        MessageEntry<Message> top = tail.get();
        while (top != null) {
            if (msg == top.message) {
                return true;
            }
            top = top.getPrev();
        }
        return false;
    }

    /**
     * Create a concurrent, parallelizable Spliterator over this queue; the
     * stream's contents will reflect the contents of this queue at the time or
     * some time after its creation (calling the spliterator's split methods
     * create a copy of some portion of the original data).
     *
     * @return A stream
     */
    @Override
    public Spliterator<Message> spliterator() {
        return new QSplit<>(tail.get());
    }

    /**
     * Create a stream over this queue; the stream's contents will reflect the
     * contents of this queue at the time or some time after its creation
     * (calling the spliterator's split methods create a copy of some portion of
     * the original data).
     *
     * @return A stream
     */
    @Override
    public Stream<Message> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Create a stream over this queue; the stream's contents will reflect the
     * contents of this queue at the time or some time after its creation
     * (calling the spliterator's split methods create a copy of some portion of
     * the original data).
     *
     * @return A stream
     */
    @Override
    public Stream<Message> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    /**
     * Equivalent to a call to <code>add(message)</code>, for compatibility with
     * queue.
     *
     * @param message An element
     * @return true
     */
    @Override
    public boolean offer(Message message) {
        return add(message);
    }

    /**
     * Remove the leading element; note that since this queue is concurrent, it
     * is entirely possible that the item you expect to remove has been removed
     * by another thread; this method follows the contract of
     * <code>Queue.remove()</code> for compatibility, but <code>pop()</code>
     * (which returns null on failure) is likely to be a better choice.
     *
     * @return The removed item
     * @throws NoSuchElementException if no items are present
     */
    @Override
    public Message remove() {
        Message result = pop();
        if (result == null) {
            throw new NoSuchElementException();
        }
        return result;
    }

    /**
     * Compatibility with Queue: Delegates to <code>pop()</code>.
     *
     * @return
     */
    @Override
    public Message poll() {
        return pop();
    }

    /**
     * Get the leading element; note that since this queue is concurrent, it is
     * entirely possible that the item you expect to remove has been removed by
     * another thread; this method follows the contract of
     * <code>Queue.remove()</code> for compatibility, but <code>peek()</code>
     * (which returns null on failure) is likely to be a better choice.
     *
     * @return The removed item
     * @throws NoSuchElementException if no items are present
     */
    @Override
    public Message element() {
        Message result = peek();
        if (result == null) {
            throw new NoSuchElementException();
        }
        return result;
    }

    /**
     * Get the leading element of the queue.
     *
     * @return An element or null
     */
    @Override
    public Message peek() {
        MessageEntry<Message> t = tail.get();
        return t == null ? null : t.message;
    }

    @Override
    public Object[] toArray() {
        return asList().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return asList().toArray(a);
    }

    /**
     * Remove - note this method delegates to <code>removeByIdentity()</code>
     * and does not do object equality checks, only instance equality.
     *
     * @param o An object
     * @return true if it was (probably) removed
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        return removeByIdentity(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return asList().containsAll(c);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean addAll(Collection<? extends Message> c) {
        if (c instanceof AtomicLinkedQueue<?>) {
            boolean wasEmpty = c.isEmpty();
            if (!wasEmpty) {
                transferContentsFrom((AtomicLinkedQueue<Message>) c);
                return true;
            }
            return false;
        }
        if (c.isEmpty()) {
            return false;
        }
        MessageEntry<Message> nue = null;
        MessageEntry<Message> first = null;
        for (Message m : c) {
            nue = new MessageEntry<>(nue, m);
            if (first == null) {
                first = nue;
            }
        }
        MessageEntry<Message> f = first;
        MessageEntry<Message> last = nue;
        tail.getAndUpdate(t -> {
            f.prev = t;
            return last;
        });
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c.isEmpty()) {
            return false;
        }
        for (;;) {
            MessageEntry<Message> t = tail.get();
            if (t == null) {
                return false;
            }
            boolean[] found = new boolean[1];
            MessageEntry<Message> nue = t.deepCopyRemovingAll(c, found);
            if (!found[0]) {
                return false;
            }
            if (tail.compareAndSet(t, nue)) {
                return true;
            }
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        for (;;) {
            MessageEntry<Message> t = tail.get();
            if (t == null) {
                return false;
            }
            boolean[] found = new boolean[1];
            MessageEntry<Message> nue = t.deepCopyRetainingAll(c, found);
            if (!found[0]) {
                return false;
            }
            if (tail.compareAndSet(t, nue)) {
                return true;
            }
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

        MessageEntry<Message> deepCopy() {
            MessageEntry<Message> p = getPrev();
            if (p != null) {
                p = p.deepCopy();
            }
            return new MessageEntry<>(p, message);
        }

        MessageEntry<Message> deepCopyRemoving(Object msg, boolean[] found) {
            if (message == msg) {
                found[0] = true;
                return prev;
            }
            MessageEntry<Message> prev = getPrev();
            MessageEntry<Message> newPrev = prev == null ? null : prev.deepCopyRemoving(msg, found);
            return new MessageEntry<>(newPrev, message);
        }

        MessageEntry<Message> deepCopyRemovingAll(Collection<?> objs, boolean[] found) {
            if (objs.contains(message)) {
                found[0] = true;
                MessageEntry<Message> prev = getPrev();
                return prev == null ? null : prev.deepCopyRemovingAll(objs, found);
            }
            MessageEntry<Message> p = getPrev();
            MessageEntry<Message> newPrev = p == null ? null
                    : p.deepCopyRemovingAll(objs, found);
            return new MessageEntry<>(newPrev, message);
        }

        MessageEntry<Message> deepCopyRetainingAll(Collection<?> objs, boolean[] found) {
            if (!objs.contains(message)) {
                found[0] = true;
                MessageEntry<Message> p = getPrev();
                return p == null ? null : p.deepCopyRetainingAll(objs, found);
            }
            MessageEntry<Message> p = getPrev();
            if (p != null) {
                p = p.deepCopyRetainingAll(objs, found);
            }
            return new MessageEntry<>(p, message);
        }

        MessageEntry<Message> copy() {
            MessageEntry<Message> p = getPrev();
            if (p == this) {
                throw new IllegalStateException("Loop on " + message);
            }
            // If we do this via recursion, the stack can overflow
            MessageEntry<Message> result = new MessageEntry<>(null, message);
            MessageEntry<Message> curr = result;
            while (p != null) {
                MessageEntry<Message> newPrev = new MessageEntry<>(null, p.message);
                curr.prev = newPrev;
                curr = newPrev;
                p = p.getPrev();
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        MessageEntry<Message> setPrev(MessageEntry<Message> prev) {
            if (prev == this) {
                throw new IllegalArgumentException("Previous cannot be self");
            }
            return (MessageEntry<Message>) UPDATER.getAndSet(this, prev);
        }

        @SuppressWarnings("unchecked")
        boolean compareAndSetPrev(MessageEntry<Message> expect, MessageEntry<Message> update) {
            return UPDATER.compareAndSet(this, expect, update);
        }

        @SuppressWarnings("unchecked")
        void updatePrev(UnaryOperator<MessageEntry<Message>> uo) {
            UPDATER.updateAndGet(this, uo);
            if (prev == this) {
                prev = null;
                throw new IllegalArgumentException("Previous cannot be self");
            }
        }

        @SuppressWarnings("unchecked")
        MessageEntry<Message> replacePrev(UnaryOperator<Message> u) {
            MessageEntry<Message> result = (MessageEntry<Message>) UPDATER.getAndUpdate(this, u);
            if (prev == this) {
                prev = null;
                throw new IllegalArgumentException("Previous cannot be self");
            }
            return result;
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

    static class QSplit<T> implements Spliterator<T> {

        private final AtomicReference<MessageEntry<T>> curr = new AtomicReference<>();

        QSplit(MessageEntry<T> tail) {
            curr.set(tail);
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            return curr.getAndUpdate(old -> {
                if (old != null) {
                    action.accept(old.message);
                    return old.prev;
                }
                return null;
            }) != null;
        }

        // This needs to be synchronized so that
        // we can't split while another split is in
        // progress and wind up with two splits with
        // the same elements, since we touch curr twice.
        @Override
        public synchronized Spliterator<T> trySplit() {
            // Get the current tail
            MessageEntry<T> origTail = curr.get();
            if (origTail == null) {
                return null;
            }
            // We will be copying all entries - at this point we are
            // immune from any changes made in the original queue
            MessageEntry<T> copy = origTail.copy();
            long sz = size(copy);
            if (sz < 2) {
                return null;
            }
            long halfwayPoint = sz / 2;
            long count = 0;
            MessageEntry<T> hp = copy;
            while (count < halfwayPoint) {
                hp = hp.getPrev();
                if (hp == null) {
                    return null;
                }
                count++;
            }
            // This detaches the tail end of our copy below the
            // halfway point from this one
            MessageEntry<T> backHalf = hp.replacePrev(old -> {
                return null;
            });
            if (backHalf == null) {
                return null;
            }
            curr.set(copy);
            return new QSplit<>(backHalf);
        }

        private long size(MessageEntry<T> e) {
            long result = 0;
            while (e != null) {
                e = e.getPrev();
                result++;
            }
            return result;
        }

        @Override
        public long estimateSize() {
            return size(curr.get());
        }

        @Override
        public int characteristics() {
            return Spliterator.CONCURRENT | Spliterator.NONNULL
                    | Spliterator.ORDERED | Spliterator.SIZED;
        }

    }
}
