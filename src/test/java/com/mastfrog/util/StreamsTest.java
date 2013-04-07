/*
 *               BSD LICENSE NOTICE
 * Copyright (c) 2010-2012, Tim Boudreau
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.mastfrog.util;

import java.util.HashSet;
import java.util.Set;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tim
 */
public class StreamsTest {
    
    @Test
    public void testAsInputStreams() throws IOException {
        ByteBuffer a = ByteBuffer.wrap(new byte[] {1,2,3,4});
        ByteBuffer b = ByteBuffer.wrap(new byte[] {5,6,7,8,9,10});
        ByteBuffer c = ByteBuffer.wrap(new byte[] {11});
        ByteBuffer d = ByteBuffer.wrap(new byte[] {});
        ByteBuffer e = ByteBuffer.wrap(new byte[] {12,13});
        ByteBuffer f = ByteBuffer.wrap(new byte[] {14});
        ByteBuffer g = ByteBuffer.wrap(new byte[] {15,16,17,18});
        ByteBuffer h = ByteBuffer.wrap(new byte[] {});
        ByteBuffer i = ByteBuffer.wrap(new byte[] {19});
        
        InputStream in = Streams.asInputStream(Arrays.asList(a,b,c,d,e,f,g,h,i));
        byte val = 0;
        for (int j = 1; j <= 19; j++) {
            int value = in.read();
            assertEquals (++val, value);
        }
        assertEquals (-1, in.read());
        a.rewind();
        b.rewind();
        c.rewind();
        d.rewind();
        e.rewind();
        f.rewind();
        g.rewind();
        h.rewind();
        i.rewind();
        
        in = Streams.asInputStream(Arrays.asList(a,b,c,d,e,f,g,h,i));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int copied = Streams.copy (in, out);
        
        byte[] found = out.toByteArray();
        byte[] expect = new byte[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
        assertArrayEquals(expect, found);
    }
    
    @Test
    public void testAsInputStream() throws IOException {
        byte[] b = new byte[238092];
        for (int i=0; i < b.length; i++) {
            b[i] = (byte) i;
        }
        ByteBuffer buf = ByteBuffer.wrap(b);
        assertTrue(true);
        InputStream in = Streams.asInputStream(buf);
        ByteArrayOutputStream out = new ByteArrayOutputStream(b.length);
        Streams.copy(in, out);
        byte[] nue = out.toByteArray();
        assertTrue (Arrays.equals(b, nue));

        in = new ByteArrayInputStream(b);
        buf = Streams.asByteBuffer(in);
        nue = new byte[buf.limit()];
        assertEquals (nue.length, b.length);
        buf.get(nue);
        assertTrue (Arrays.equals(b, nue));

        File f = File.createTempFile(StreamsTest.class.getName() + System.currentTimeMillis(), "tmp");
        FileOutputStream fos = new FileOutputStream (f);
        in = new ByteArrayInputStream(b);
        Streams.copy(in, fos);
        fos.close();
        in.close();
        
        FileInputStream fis = new FileInputStream(f);
        FileChannel chan = fis.getChannel();
        try {
            in = Streams.asInputStream(chan);
            
            out = new ByteArrayOutputStream(b.length);
            Streams.copy(in, out);

            nue = out.toByteArray();
            assertTrue (Arrays.equals(b, nue));
        } finally {
            chan.close();
        }
        f.delete();
    }

    @Test
    public void testLink() throws IOException {
        File tmp = new File (System.getProperty("java.io.tmpdir"));
        File testDir = new File (tmp, getClass().getName() + '_' + Long.toString(System.currentTimeMillis(), 36));
        assertTrue (testDir.mkdir());
        File srcDir = new File (testDir, "a/b/c/d");
        assertTrue (srcDir.mkdirs());
        File destDir = new File (testDir, "1/2/3/4");
        assertTrue (destDir.mkdirs());
        File linkTo = new File (srcDir, "x.txt");
        assertTrue (linkTo.createNewFile());
        PrintWriter pw = new PrintWriter(new FileOutputStream (linkTo));
        pw.println ("I will be linked");
        pw.close();
        File link = new File (destDir, "y.txt");
        Streams.link(linkTo, link);
        assertTrue (link.exists());
        String s = Streams.readString(new FileInputStream(link));
        assertEquals ("I will be linked", s.trim());
    }
}
