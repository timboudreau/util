package com.mastfrog.util;

import com.mastfrog.util.streams.HashingInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author tim
 */
public class HashingInputStreamTest {

    @Test
    public void testMarkSupported() throws Throwable {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int j = 0; j < 10; j++) {
            for (int i = 0; i < 256; i++) {
                out.write(i);
            }
        }
        byte[] bytes = out.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        HashingInputStream hin = HashingInputStream.sha1(in);

        ByteArrayOutputStream two = new ByteArrayOutputStream();

        int copied = Streams.copy(hin, two);
        assertEquals (bytes.length, copied);
        hin.close();

        byte[] nue = two.toByteArray();

        assertTrue(true);

        assertArrayEquals(bytes, nue);

        assertNotNull(hin.getDigest());

        assertEquals(bytes.length, nue.length);

    }
}
