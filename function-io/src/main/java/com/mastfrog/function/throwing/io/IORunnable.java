package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.ThrowingRunnable;
import java.io.IOException;

/**
 * IOException specialization of ThrowingRunnable.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IORunnable extends ThrowingRunnable {
    public static final IORunnable NO_OP = new NoOpIORunnable();

    @Override
    public void run() throws IOException;

    default IORunnable andThen(IORunnable next) {
        return () -> {
            IORunnable.this.run();
            next.run();
        };
    }

    @Override
    default IORunnable andThen(Runnable next) {
        return () -> {
            IORunnable.this.run();
            next.run();
        };
    }
}
