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

package com.mastfrog.file.channels;

import java.io.IOException;

/**
 * An IOException that can provide information about the
 * lease being used when it was thrown.
 *
 * @author Tim Boudreau
 */
public final class LeaseException extends IOException {

    private Lease lease;
    private long pos;

    public LeaseException() {
    }

    public LeaseException(String message) {
        super(message);
    }

    public LeaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public LeaseException(Throwable cause) {
        super(cause);
    }

    public void setLease(Lease lease, long pos) {
        this.lease = lease;
        this.pos = pos;
    }

    @Override
    public String getMessage() {
        String result = super.getMessage();
        if (lease != null) {
            result += " @" + pos + " in " + lease;
        }
        return result;
    }

}
