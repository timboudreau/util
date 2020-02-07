/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.function;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface BooleanConsumer {

    void accept(boolean val);

    default Runnable toRunnable(BooleanSupplier supplier) {
        return () -> accept(supplier.getAsBoolean());
    }

    default BooleanConsumer andThen(BooleanConsumer other) {
        return new BooleanConsumer() {
            List<BooleanConsumer> others = new ArrayList<>(2);

            @Override
            public void accept(boolean val) {
                BooleanConsumer.this.accept(val);
                for (BooleanConsumer b : others) {
                    b.accept(val);
                }
            }

            public String toString() {
                return BooleanConsumer.this + ", " + others;
            }

            @Override
            public BooleanConsumer andThen(BooleanConsumer other) {
                others.add(other);
                return this;
            }
        };
    }

    static BooleanConsumer ifTrue(Runnable run) {
        return new BooleanConsumer() {

            @Override
            public void accept(boolean val) {
                if (val) {
                    run.run();
                }
            }

            @Override
            public String toString() {
                return "ifTrue(" + run + ")";
            }
        };
    }

    static BooleanConsumer ifFalse(Runnable run) {
        return new BooleanConsumer() {

            @Override
            public void accept(boolean val) {
                if (!val) {
                    run.run();
                }
            }

            @Override
            public String toString() {
                return "ifTrue(" + run + ")";
            }
        };
    }
}
