package com.mastfrog.concurrent.lock;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.BooleanSupplier;

/**
 * A EntryTracker.
 *
 * @author Tim Boudreau
 */
final class EntryTracker {

    private final AtomicIntegerArray maxes;
    private final AtomicIntegerArray entryCounts;

    EntryTracker(int ct) {
        this.maxes = new AtomicIntegerArray(ct);
        this.entryCounts = new AtomicIntegerArray(ct);
    }

    boolean run(int ix, BooleanSupplier run) {
        int entries = entryCounts.incrementAndGet(ix);
        try {
            maxes.getAndUpdate(ix, old -> {
                return Math.max(entries, old);
            });
            return run.getAsBoolean();
        } finally {
            entryCounts.decrementAndGet(ix);
        }
    }

    public String maximums() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxes.length(); i++) {
            int max = maxes.get(i);
            if (max > 0) {
                sb.append(i).append(":").append(max).append(", ");
            }
        }
        return sb.toString();
    }

}
