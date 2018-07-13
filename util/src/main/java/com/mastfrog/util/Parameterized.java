package com.mastfrog.util;

/**
 * Base class for things with a single parameter which need to provide type
 * information about that parameter at runtime.
 *
 * @author Tim Boudreau
 */
public interface Parameterized<T> {

    Class<T> type();

    public static abstract class Abstract<T> implements Parameterized<T> {

        private final Class<T> type;

        public Abstract(Class<T> type) {
            this.type = type;
        }

        @Override
        public final Class<T> type() {
            return type;
        }
    }
}
