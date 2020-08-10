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

/**
 * A general abstraction for the following situation: You have lots of objects
 * (such as files), and multiple parties may be interested in changes, parses,
 * or some sort of events on them - <i>and</i> those objects may be referenced
 * via more than one type (for example, the Document with the contents of a file
 * versus the file represented by a java.io.File, java.nio.Path or
 * org.netbeans.filesystems.FileObject).
 * <p>
 * SubscribableBuilder offers a wildly configurable means of building these,
 * including synchronous vs. asynchronous vs. coalesced asynchronous delivery of
 * events, customizable map (or other storage) backed storage, with strongly or
 * weakly keys and values, and more.
 * </p>
 * The SPI class SubscribableNotifier is used to deliver events, so that clients
 * that consume events are not exposed to the API for publishing events.
 *
 * @author Tim Boudreau
 */
public interface Subscribable<K, C> {

    /**
     * Subscribe one subscriber to events on one object of type K.
     *
     * @param key The object
     * @param consumer A consumer of some sort of event on it
     */
    void subscribe(K key, C consumer);

    /**
     * Unsubscribe one subscriber from events on one object of type K.
     *
     * @param key The object whose events are listened to
     * @param consumer A consumer that should no longer be notified
     */
    void unsubscribe(K key, C consumer);

    /**
     * Remove all subscribers from events on one object of type K.
     *
     * @param key The object
     */
    void destroyed(K key);
}
