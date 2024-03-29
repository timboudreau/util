/*
 * The MIT License
 *
 * Copyright 2023 Mastfrog Technologies.
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
package com.mastfrog.concurrent.stats;

import java.util.Optional;

/**
 * Bookkeeping for StatisticCollector.compute().
 *
 * @author Tim Boudreau
 */
final class ComputationEntry<S extends StatisticComputation<ValueConsumer, C, N>, ValueConsumer, C extends ValueConsumer, N extends Number> {

    private final S computation;
    private final C mapper;

    ComputationEntry(S computation) {
        this.computation = computation;
        mapper = computation.map();
    }

    static <S extends StatisticComputation<ValueConsumer, C, N>, ValueConsumer, C extends ValueConsumer, N extends Number>
            ComputationEntry<S, ValueConsumer, C, N> newComputationEntry(S computation) {
        return new ComputationEntry<>(computation);
    }

    ValueConsumer consumer() {
        return mapper;
    }

    Optional<N> result() {
        return computation.reduce(mapper);
    }
}
