/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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
package com.mastfrog.util.libversion;

import static com.mastfrog.util.libversion.VersionInfo.GMT;
import com.mastfrog.util.strings.Strings;
import java.time.ZonedDateTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class VersionInfoTest {

    @Test
    public void testStuff() {
        ZonedDateTime z = ZonedDateTime.now().withZoneSameInstant(GMT);

        String got = z.format(VersionInfo.ISO_INSTANT);
        System.out.println(got);

        ZonedDateTime z1 = ZonedDateTime.parse(got);
        assertEquals(z.toInstant(), z1.toInstant());
    }

    @Test
    public void testInfoLookup() {
        VersionInfo info = VersionInfo.find(Strings.class, "com.mastfrog", "util-strings");
        System.out.println("GOT VERSION " + info);
        assertNotNull(info);
        assertEquals("com.mastfrog", info.groupId);
        assertEquals("util-strings", info.artifactId);
        assertNotEquals("-", info.shortCommitHash);
        assertNotEquals("-", info.longCommitHash);
        assertNotNull(info.commitDate);
        assertNotSame(VersionInfo.EPOCH, info.commitDate);
        assertTrue(info.foundGitMetadata);
        assertTrue(info.foundMavenMetadata);

        VersionInfo b = new VersionInfo(Strings.class, "com.mastfrog", "util-strings", true);
        assertEquals("com.mastfrog", b.groupId);
        assertEquals("util-strings", b.artifactId);
        assertEquals("-", b.shortCommitHash);
        assertEquals("-", b.longCommitHash);
        assertNotNull(b.commitDate);
        assertSame(VersionInfo.EPOCH, b.commitDate);
        assertEquals(info.version, b.version);
    }

}
