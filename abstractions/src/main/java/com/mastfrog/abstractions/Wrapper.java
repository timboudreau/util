package com.mastfrog.abstractions;

/**
 * Abstraction for an object which is a wrapper for another object, which may be
 * of the same or a different type, and may exist in a hierarchy of such
 * objects, with methods for resolving such wrapped objects in a type-safe way.
 *
 * @author Tim Boudreau
 */
public interface Wrapper<W> extends Named {

    /**
     * Get the object wrapped by this one.
     *
     * @return The wrapped object or null
     */
    W wrapped();

    @SuppressWarnings("unchecked")
    default W root() {
        W wrapped = wrapped();
        while (wrapped instanceof Wrapper<?>) {
            wrapped = ((Wrapper<W>) wrapped).wrapped();
        }
        return wrapped;
    }

    /**
     * Finds any instance of the Named interface, and returns the result if its
     * name() method if present, and if not, returns toString() on the wrapped
     * object.
     *
     * @return A name or toString() value, or "null" if the wrapped object is
     * null
     */
    @Override
    default String name() {
        Named n = find(wrapped(), Named.class);
        if (n != null) {
            return n.name();
        }
        W wrapped = wrapped();
        return wrapped == null ? "null" : wrapped.toString();
    }

    @SuppressWarnings("unchecked")
    static <P1> P1 root(P1 o) {
        if (o instanceof Wrapper<?>) {
            return ((Wrapper<P1>) o).root();
        }
        return (P1) o;
    }

    @SuppressWarnings("unchecked")
    static <F> F find(Object o, Class<? super F> what) {
        if (o == null) {
            return null;
        }
        if (o instanceof Wrapper<?>) {
            return ((Wrapper<?>) o).find(what);
        }
        if (what.isInstance(o)) {
            return (F) what.cast(o);
        }
        return null;
    }

    default <F> boolean has(Class<? super F> what) {
        return find(what) != null;
    }

    @SuppressWarnings("unchecked")
    default <F> F find(Class<? super F> what) {
        if (what.isInstance(this)) {
            return (F) this;
        }
        W wrapped = wrapped();
        if (what.isInstance(wrapped)) {
            return (F) wrapped;
        }
        if (wrapped instanceof Wrapper<?>) {
            return ((Wrapper<?>) wrapped).find(what);
        }
        return null;
    }
}
