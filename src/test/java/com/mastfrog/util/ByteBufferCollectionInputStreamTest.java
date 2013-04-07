package com.mastfrog.util;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class ByteBufferCollectionInputStreamTest {

    @Test
    public void testRead() throws Exception {
        assertTrue(true);
        StringBuilder sb = new StringBuilder();
        ByteBuffer a = buffer('a', 23, sb);
        ByteBuffer b = buffer('b', 52, sb);
        ByteBuffer c = buffer('c', 17, sb);
        InputStream in = new ByteBufferCollectionInputStream(a, b, c);
        String s = Streams.readString(in, 16);
        assertEquals(sb.toString(), s);

        // test various pathologies

        in = new ByteBufferCollectionInputStream();
        s = Streams.readString(in, 20);
        assertEquals("", s);

        in = new ByteBufferCollectionInputStream();
        s = Streams.readString(in, 40);
        assertEquals("", s);

        sb = new StringBuilder();
        in = new ByteBufferCollectionInputStream(ByteBuffer.allocate(0), ByteBuffer.allocate(0), buffer('a', 17, sb));
        s = Streams.readString(in, 15);
        assertEquals(sb.toString(), s);

        sb = new StringBuilder();
        in = new ByteBufferCollectionInputStream(ByteBuffer.allocate(0), ByteBuffer.allocate(0), buffer('d', 13, sb), ByteBuffer.allocate(0));
        s = Streams.readString(in, 10);
        assertEquals(sb.toString(), s);

        sb = new StringBuilder();
        List<ByteBuffer> l = new LinkedList<>();
        for (char cc : new char[]{'a', 'q', '3'}) {
            l.add(buffer(cc, 383, sb));
        }
        in = new ByteBufferCollectionInputStream(l);
        s = Streams.readString(in);
        assertEquals(sb.toString(), s);
    }

    private ByteBuffer buffer(char c, int count, StringBuilder sb) throws Exception {
        byte[] b = new byte[count];
        Arrays.fill(b, (byte) c);
        sb.append(new String(b, "US-ASCII"));
        return ByteBuffer.wrap(b);
//        ByteBuffer res = ByteBuffer.allocateDirect(Math.max(128, count));
//        res.put(b);
//        return res;
    }
}
