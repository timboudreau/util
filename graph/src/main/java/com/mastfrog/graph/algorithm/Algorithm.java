package com.mastfrog.graph.algorithm;

import com.mastfrog.graph.IntGraph;

/**
 *
 * @author Tim Boudreau
 */
public abstract class Algorithm<R, A extends Algorithm> {

    public abstract R apply(IntGraph graph);

    public A setParameter(IntParameter<A> param, int value) {
        throw new IllegalArgumentException("Param " + param + " not supported");
    }

    public A setParameter(DoubleParameter<A> param, double value) {
        throw new IllegalArgumentException("Param " + param + " not supported");
    }

    public A setParameter(BooleanParameter<A> param, boolean value) {
        throw new IllegalArgumentException("Param " + param + " not supported");
    }

    protected static <A extends Algorithm> DoubleParameter<A> createDoubleParameter(Class<A> alg, String name) {
        return new DoubleParameter<>(alg, name);
    }

    protected static <A extends Algorithm> IntParameter<A> createIntegerParameter(Class<A> alg, String name) {
        return new IntParameter<>(alg, name);
    }

    protected static <A extends Algorithm> BooleanParameter<A> createBooleanParameter(Class<A> alg, String name) {
        return new BooleanParameter<>(alg, name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    static abstract class Parameter<A extends Algorithm> {

        private final Class<A> owner;
        private final String name;

        private Parameter(Class<A> owner, String name) {
            this.owner = owner;
            this.name = name;
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return name + " " + getClass().getSimpleName() + " of " + owner.getSimpleName();
        }
    }

    public static class DoubleParameter<A extends Algorithm> extends Parameter<A> {

        private DoubleParameter(Class<A> type, String name) {
            super(type, name);
        }
    }

    public static class BooleanParameter<A extends Algorithm> extends Parameter<A> {

        private BooleanParameter(Class<A> type, String name) {
            super(type, name);
        }
    }

    public static class IntParameter<A extends Algorithm> extends Parameter<A> {

        private IntParameter(Class<A> type, String name) {
            super(type, name);
        }
    }

    public static EigenvectorCentrality eigenvectorCentrality() {
        return new EigenvectorCentrality();
    }

    public static PageRank pageRank() {
        return new PageRank();
    }

}
