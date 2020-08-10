/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
