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
package com.mastfrog.logstructured;

import com.mastfrog.file.channels.FileChannelPool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
class CursorFileTest {

    private Path path;

    @Test
    void testCreate() throws Throwable {
        assertFalse(Files.exists(path));
        CursorFile cf = new CursorFile(path, pool);
        assertEquals(-1, cf.read());
        CursorFileWriter w = cf.newWriter();
        w.accept(2305L);
        assertEquals(2305L, cf.read());
        w.accept(5205L);
        assertEquals(5205L, cf.read());
        try {
            w.accept(-30L);
            fail("negative value accepted");
        } catch (IllegalArgumentException ex) {

        }
        assertEquals(5205L, cf.read());

        CursorFile cf2 = new CursorFile(path, pool);
        assertEquals(5205L, cf2.read(), "Value not persisted");
    }

    private FileChannelPool pool;

    @BeforeEach
    void before() {
        pool = FileChannelPool.newPool(5000);
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        path = tmp.resolve(getClass().getSimpleName() + "-"
                + Long.toString(System.currentTimeMillis(), 36)
                + ".cursor");
    }

    @AfterEach
    void after() throws IOException {
        if (Files.exists(path)) {
            Files.delete(path);
        }
        pool.close();
    }
}
