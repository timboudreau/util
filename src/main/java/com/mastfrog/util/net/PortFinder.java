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
package com.mastfrog.util.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Utility class for finding an unused port.
 *
 * @author Tim Boudreau
 */
public final class PortFinder {

    private final int last;
    private final int first;
    // With parallel tests, using the wall clock as a seed can get us
    // the same port
    private static final SecureRandom SRAND = new SecureRandom();
    private final Random random = new Random(SRAND.nextInt());
    private final int maxIterations;

    public PortFinder() {
        this(nextStartPort(), 65535);
    }

    private static final int BASE_START_PORT = 4000;
    private static int LAST_START_PORT = BASE_START_PORT;
    static synchronized int nextStartPort() {
        int result = LAST_START_PORT + 200;
        LAST_START_PORT = result;
        if (LAST_START_PORT > 65535) {
            LAST_START_PORT = BASE_START_PORT;
        }
        return result;
    }

    public PortFinder(int first, int last) {
        this(first, last, 2000);
    }

    public PortFinder(int first, int last, int maxIterations) {
        check("first", first);
        check("last", last);
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("maxIterations too low: " + maxIterations);
        }
        if (last <= first) {
            throw new IllegalArgumentException("Last is less than first: "
                    + first + " thru " + last);
        }
        this.first = first;
        this.last = last;
        this.maxIterations = maxIterations;
    }

    private void check(String name, int port) {
        if (port <= 0) {
            throw new IllegalArgumentException("Negative port " + port + " for " + name);
        }
        if (port > 65535) {
            throw new IllegalArgumentException(name + " port must be <= 65535");
        }
        if (port == 0) {
            throw new IllegalArgumentException(name + " port may not be zero");
        }
    }

    /**
     * Get an unused port.
     * 
     * @return A port number.
     * @throws IllegalStateException if the max iteration count is exceeded without
     * finding an available port.
     */
    public int findAvailableServerPort() {
        return findPort();
    }

    private int findPort() {
        int port = nextStartPort();
        int iterations = 0;
        do {
            port += random.nextInt(25);
            if (port > last) {
                port = first;
            }
            iterations++;
            if (iterations == maxIterations) {
                throw new IllegalStateException("Max iterations exceeded but "
                        + "no available port found between " + first + " and "
                        + "last");
            }
        } while (!available(port));
        return port;
    }

    private boolean available(int port) {
        try (ServerSocket ss = new ServerSocket(port)) {
            ss.setReuseAddress(true);
            try (DatagramSocket ds = new DatagramSocket(port)) {
                ds.setReuseAddress(true);
                return true;
            } catch (IOException e) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }
}
