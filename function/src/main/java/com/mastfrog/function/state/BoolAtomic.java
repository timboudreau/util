/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.function.state;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Tim Boudreau
 */
final class BoolAtomic implements Bool {

    private final AtomicBoolean value;

    BoolAtomic(boolean value) {
        this.value = new AtomicBoolean(value);
    }

    BoolAtomic() {
        this.value = new AtomicBoolean();
    }

    @Override
    public boolean set() {
        return value.compareAndSet(false, true);
    }

    @Override
    public boolean reset() {
        return !value.getAndSet(false);
    }

    @Override
    public boolean runAndSet(Runnable r) {
        if (value.compareAndSet(false, true)) {
            r.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean set(boolean val) {
        boolean result = value.compareAndSet(!val, val);
        return result ? !val : val;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public boolean toggle() {
        // XXX should loop here until we successfully change
        // the value.
        boolean old = value.get();
        value.set(!old);
        return old;
    }

    @Override
    public void accept(boolean val) {
        value.set(val);
    }

    @Override
    public boolean getAsBoolean() {
        return value.get();
    }
}
