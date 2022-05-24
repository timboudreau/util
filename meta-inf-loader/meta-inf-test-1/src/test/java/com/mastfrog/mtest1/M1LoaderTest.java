/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.mtest1;

import com.mastfrog.metainf.MetaInfLoader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class M1LoaderTest {

    @Test
    public void testResourceExists() throws Exception {
        M1X.tryStuff();
    }

    @Test
    public void assertRunningInModuleSystem() {
        assertNotNull(getClass().getModule().getLayer(), "Null module layer - check surefire config?");
        assertTrue(getClass().getModule().isNamed(), "Not in a named module: " + getClass().getModule());
    }

    @Test
    public void testResourceExposed() throws Exception {
        // TODO review the generated test code and remove the default call to fail.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterable<InputStream> items = MetaInfLoader.loadAll("META-INF/stuff/stuff.txt");
        int ct = 0;
        for (InputStream in : items) {
            ct++;
            try ( InputStream i = in) {
                com.mastfrog.util.streams.Streams.copy(i, baos);
            }
        }
        String content = new String(baos.toByteArray(), UTF_8);
        System.out.println("CONTENT: '" + content + "'");

        assertTrue(M1Loader.instanceCreated, "Instance of m1 loader not created.  Content: " + content);
        assertEquals(4, ct, "Should have found at least four streams.  Content: " + content);
        assertEquals("Test one has stuff.\n"
                + "Test 2 has stuff too.\n"
                + "Test 3's stuff.\n"
                + "Test 4's non-modular stuff.\n",
                content, "Content did not match expectation");

    }
}
