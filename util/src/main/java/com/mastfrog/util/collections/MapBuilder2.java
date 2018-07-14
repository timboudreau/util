/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
package com.mastfrog.util.collections;

import com.mastfrog.util.preconditions.Exceptions;
import com.mastfrog.util.multivariate.Pair;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author Tim Boudreau
 */
public interface MapBuilder2<T, R> {

    ValueBuilder<T, R> map(T key);

    default MapBuilder2<T, R> map(T key, R val) {
        return map(key).to(val);
    }

    public interface ValueBuilder<T, R> {

        MapBuilder2<T, R> to(R value);

        Map<T, R> finallyTo(R value);
    }

    Map<T, R> build();

    Map<T, R> buildLinkedHashMap();

    Map<T, R> buildImmutableMap();

    public default HashingMapBuilder<T, R> toHashingMapBuilder(String algorithm) {
        return toHashingMapBuilder(algorithm, (o) -> {
            return o.toString().getBytes(UTF_8);
        });
    }

    public default HashingMapBuilder<T, R> toHashingMapBuilder(String algorithm, Function<Object, byte[]> toBytes) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(algorithm);
            final MapBuilder2<T, R> outer = this;
            return new HashingMapBuilder<T, R>() {
                byte[] digested;

                @Override
                public byte[] hash() {
                    if (digested == null) {
                        digested = digest.digest();
                    }
                    return digested;
                }

                @Override
                public String hashString() {
                    Base64.Encoder enc = Base64.getEncoder();
                    return enc.encodeToString(hash());
                }

                @Override
                public HashingValueBuilder<T, R> map(T key) {
                    if (digested != null) {
                        throw new IllegalStateException("Hash already computed");
                    }
                    HashingMapBuilder<T, R> otr = this;
                    return new HashingValueBuilder<T, R>() {
                        @Override
                        public HashingMapBuilder<T, R> to(R value) {
                            if (digested != null) {
                                throw new IllegalStateException("Hash already computed");
                            }
                            outer.map(key).to(value);
                            digest.update(toBytes.apply(key));
                            digest.update((byte) ':');
                            digest.update(toBytes.apply(value));
                            digest.update((byte) '/');
                            return otr;
                        }

                        @Override
                        public Map<T, R> finallyTo(R value) {
                            to(value);
                            return outer.build();
                        }

                        @Override
                        public Pair<Map<T, R>, byte[]> toAndBuild(R val) {
                            HashingMapBuilder<T, R> result = to(val);
                            return Pair.<Map<T, R>, byte[]>from(result.build()).apply(hash());
                        }
                    };
                }

                @Override
                public Map<T, R> build() {
                    hash();
                    return outer.build();
                }

                @Override
                public Map<T, R> buildLinkedHashMap() {
                    return outer.buildLinkedHashMap();
                }

                @Override
                public Map<T, R> buildImmutableMap() {
                    return outer.buildImmutableMap();
                }
            };
        } catch (NoSuchAlgorithmException ex) {
            return Exceptions.chuck(ex);
        }
    }

    public interface HashingMapBuilder<T, R> extends MapBuilder2<T, R> {

        byte[] hash();

        String hashString();

        @Override
        HashingValueBuilder<T, R> map(T obj);

        public interface HashingValueBuilder<T, R> extends ValueBuilder<T, R> {

            HashingMapBuilder<T, R> to(R obj);

            Pair<Map<T, R>, byte[]> toAndBuild(R val);

            default Pair<Map<T, R>, String> toAndBuildWithStringHash(R val) {
                Base64.Encoder enc = Base64.getEncoder();
                return toAndBuild(val).transformB(enc::encodeToString);
            }
        }
    }
}
