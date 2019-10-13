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
package com.mastfrog.util.path;

import com.mastfrog.util.file.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class UnixPathTest {

    String[] TO_TEST = new String[]{
        "com/foo/bar",
        "moopy",
        "a/b/c/d/e/f/g/h/i/j/k",
        "/a/b/c/d/e/f/g/h/i/j/k",
        "/rooty/tooty",
        "/rooty",
        "/",
        "",
        "a0/b0/c0/../../a0b1/c1/b3", // a0/a0b1/c1/b3
        "/a0/b0/c0/../../a0b1/c1/b3", // /a0/a0b1/c1/b3
        "a/b/c/./d/e/f/./g/h/./i", //
        "/a/b/c/./d/e/f/./g/h/./i",
        "/a/b/c/./d/e/f/./g/h/../../g1",
        "/./a/b/c/d/e/../../d1/e1/../../d2/e2/../../../c1/d3/e3", // /a/b/c1/d3/e3
        "/./a/b/./././././././././././..", // /a
        "../../../foo",
        "foo/bar/../..",
        "foo/bar/../../..",
        "..",
        "/..",
        ".",
        "././././././././././",
        "./././././././././.",
        "././././././././././x/y/z",
        "foo///bar///baz/x",
        "foo///bar///../baz/x",
        "foo///bar///./../baz/x",
        "///",
        ".///",
        "x///",
        "x///."
    };

    UPathFactory factory;

    @Test
    public void testRelativize() {
        testRelativize("a/b/c/d/e/f/g/h/i", "a/b/c/d"); // "e/f/g/h/i");
        testRelativize("a/b/c/d", "a/b/c/d/e/f/g/h/i"); // "../../../../..");
        testRelativize("a/b/c/d/e", "a/b/c1/d1/e1"); // , "../../../c1/d1/e1");
        testRelativize("a/b/c1/d1/e1", "a/b/c/d/e"); // , "../../../c1/d1/e1");
        testRelativize("a/b/c/d", "e/f/g/h/i");
        testRelativize("a/b/c/d/e", "f/g/h/i");
        testRelativize("a/b/c/d", "e/f/g/h");
        testRelativize("a/b/c/d", "a/b/c/d");
        testRelativize("", "a/b/c/d");
        testRelativize("a/b/c/d", "");
        testRelativize("", "");
        testRelativize("/", "/");
        testRelativize("/a/b/c/d", "/a/b/c/d/e");
        testRelativize("/a/b/c/d/e", "/a/b/c/d");
        testRelativize("/a/b/c/d", "/a/b/c/d");
        testRelativize("/a////b/c/d", "/a////b/c/d");
        testRelativize("/a/b/c/de/fg", "/a/b/c/d");
        testRelativize("/a////b/c/de/fg", "/a////b/c/d");
        testRelativize("/d/", "/a/b/c/d/e/f/g");
        testRelativize("g/f/e/d/c/b/a", "a/b/c/d/e/f/g");
        testRelativize("a/b/c/d/./e/f/g", "a/b/c/d/e/./f/g");
        testRelativize("a/b/c/d/./e/f/g", "a/b/c/d/e/f/g");
    }

    private void testRelativize(String pth, String relativeTo) {
        if (!UnixPaths.isNativeUnixPaths()) {
            return;
        }
        Path p = Paths.get(pth);
        Path r = Paths.get(relativeTo);
        Path g = r.relativize(p);

        String exp = g == null ? null : g.toString();

        ComponentUPath path = ComponentUPath.ofUnixPath(pth);
        ComponentUPath rel = ComponentUPath.ofUnixPath(relativeTo);
        try {
            ComponentUPath x = rel.relativize(path);
            String xval = x == null ? null : x.toString();

            System.out.println(relativeTo + ".relativize(" + pth + ")='" + exp + "' got '" + xval + "'");
            assertEquals("Relativize '" + pth + "' on '" + relativeTo + "' gets "
                    + "wrong result '" + xval + "' with '" + rel + "' and '" + path
                    + "'\n" + x.infoString(), exp, xval);

        } catch (Exception ex) {
            throw new AssertionError("Failure relativizing " + path.infoString()
                    + " and " + rel.infoString() + " for '" + pth + "' and '"
                    + rel + "'", ex);
        }
    }

    @Test
    public void testConsecutiveSlashesAreNormalized() {
        if (!UnixPaths.isNativeUnixPaths()) {
            return;
        }
        ComponentUPath cup = ComponentUPath.ofUnixPath("a/b/c");
        ComponentUPath cup2 = ComponentUPath.ofUnixPath("a//b/c");
        assertEquals(cup, cup2);
        cup2 = ComponentUPath.ofUnixPath("a///b/c");
        assertEquals(cup, cup2);
        cup2 = ComponentUPath.ofUnixPath("a///b/c/");
        assertEquals(cup, cup2);
    }

    @Test
    public void testComponentNormalization() {
        if (!UnixPaths.isNativeUnixPaths()) {
            return;
        }
        ComponentUPath cup = ComponentUPath.ofUnixPath("a/b/c/./d/e/f/./g/h/./i");
        assertEquals("a/b/c/./d/e/f/./g/h/./i", cup.toString());
        assertEquals("a/b/c/d/e/f/g/h/i", cup.normalize().toString());

        cup = ComponentUPath.ofUnixPath("a/b/c/../d");
        assertEquals("a/b/c/../d", cup.toString());
        assertEquals("a/b/d", cup.normalize().toString());
    }

    @Test
    public void testComponentUPathBasic() {
        if (!UnixPaths.isNativeUnixPaths()) {
            return;
        }
        ComponentUPath cpu = ComponentUPath.of("com/mastfrog/UPathTest.java");
        assertNotNull(cpu);
        assertEquals("com/mastfrog/UPathTest.java", cpu.toString());
        assertEquals(3, cpu.getNameCount());
        assertNotNull("com", cpu.getName(0));
        assertEquals("com", cpu.getName(0).toString());
        assertNotNull("mastfrog", cpu.getName(1));
        assertEquals("mastfrog", cpu.getName(1).toString());
        assertNotNull("UPathTest.java", cpu.getName(2));
        assertEquals("UPathTest.java", cpu.getName(2).toString());
        assertFalse(cpu.isAbsolute());
        assertSame(cpu, cpu.normalize());
        ComponentUPath par = cpu.getParent();
        assertNotNull(par);
        assertEquals("com/mastfrog", par.toString());
        assertFalse(par.isAbsolute());
        assertSame(par, par.normalize());

        ComponentUPath abs = cpu.toAbsolutePath();
        Path p = Paths.get(".").toAbsolutePath().normalize().resolve("com/mastfrog/UPathTest.java");

        assertNotNull(abs);
        assertNotSame(abs, cpu);
        assertTrue(abs.isAbsolute());
        assertEquals(p.toString(), abs.toString());
    }

    @Test
    public void testComparableWorksSimilarly() {
        List<String> expectedSorted = sortedPaths(Arrays.asList(TO_TEST), Paths::get);
//        System.out.println("\nEXP SORTED:");
//        for (String s : expectedSorted) {
//            System.out.println(" - '" + s + "'");
//        }
        List<String> gotSorted = sortedPaths(Arrays.asList(TO_TEST), ComponentUPath::of);
//        System.out.println("\nGOT SORTED:");
//        for (String s : gotSorted) {
//            System.out.println(" - '" + s + "'");
//        }

        assertListsEqual(expectedSorted, gotSorted);
        List<String> wrapSorted = sortedPaths(Arrays.asList(TO_TEST), str -> WrappedUPath.of(Paths.get(str)));
//        assertListsEqual(expectedSorted, wrapSorted);
    }

    private void assertListsEqual(List<String> exp, List<String> got) {
        StringBuilder sb = new StringBuilder();
        assertEquals(exp.size(), got.size());
        for (int i = 0; i < exp.size(); i++) {
            if (!exp.get(i).equals(got.get(i))) {
                sb.append(i).append(". ").append("Exp: '").append(exp.get(i))
                        .append("'\n")
                        .append(i).append(". ").append("Got '").append(got.get(i)).append("'\n");
            }
        }
        if (sb.length() > 0) {
            fail(sb.insert(0, "Lists differ:\n").toString());
        }
    }

    public List<String> sortedPaths(List<String> names, Function<String, Path> f) {
        List<Path> paths = new ArrayList<>(names.size());
        for (String n : names) {
            Path p = f.apply(n);
            paths.add(p);
        }
        Collections.sort(paths);
        List<String> result = new ArrayList<>(paths.size());
        for (Path p : paths) {
            result.add(p.toString());
        }
        return result;
    }

    @Test
    public void testBehaviorComparableToUnixPath() {
        if (!UnixPaths.isNativeUnixPaths()) {
            return;
        }
        factory = new CompFactory();
        testAll();
        factory = new WrapFactory();
        testAll();
    }

    @Test
    public void testPathTranslatesBackToSystemPathCorrectly() throws IOException {
        Path pth = FileUtils.newTempFile("UnixPathTest");
        try {
            UnixPath p = UnixPath.get(pth);
            Random rnd = ThreadLocalRandom.current();
            byte[] b = new byte[1024];
            rnd.nextBytes(b);
            Files.write(p.toNativePath(), b, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            byte[] b1 = Files.readAllBytes(p.toNativePath());
            Assert.assertArrayEquals(b, b1);
        } finally {
            FileUtils.deleteIfExists(pth);
        }
    }

    @Test
    public void testVisitNames() {
        String exp = "com/foo/bar/baz/whatever.txt";
        UnixPath up = ComponentUPath.ofUnixPath(exp);
        StringBuilder sb = new StringBuilder();
        up.visitNames(comp -> {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(comp);
        });
        assertEquals(exp, sb.toString());
    }

    @Test
    public void testRawName() {
        String exp = "com/foo/bar/baz/whatever.txt";
        UnixPath up = ComponentUPath.ofUnixPath(exp);
        assertEquals("whatever", up.rawName());
        up = ComponentUPath.ofUnixPath(exp + "/./.");
        assertEquals("whatever", up.rawName());

        exp = "com/foo/bar/baz/whatever";
        up = ComponentUPath.ofUnixPath(exp);
        assertEquals("whatever", up.rawName());
        exp = "com/foo/bar/baz/whatever/././..";
        up = ComponentUPath.ofUnixPath(exp);
        assertEquals("baz", up.rawName());

        exp = "com/foo/bar/baz/whatever/";
        up = ComponentUPath.ofUnixPath(exp);
        assertEquals("whatever", up.rawName());

        assertEquals("", up.extension());
        assertFalse(up.isExtension("whatever"));

        exp = "";
        up = ComponentUPath.ofUnixPath(exp);
        assertEquals("", up.extension());
        assertEquals("", up.rawName());
    }

    @Test
    public void testExtension() {
        String exp = "com/foo/bar/baz/whatever.txt";
        UnixPath up = ComponentUPath.ofUnixPath(exp);
        assertEquals("txt", up.extension());
        assertTrue(up.isExtension("txt"));
        assertTrue(up.isExtension(".txt"));
        up = ComponentUPath.ofUnixPath(exp + "/./.");
        assertEquals("txt", up.extension());
        assertTrue(up.isExtension("txt"));
        assertTrue(up.isExtension(".txt"));
        assertFalse(up.isExtension(".txtr"));
        assertFalse(up.isExtension(".tx"));
        assertFalse(up.isExtension(".txg"));
        assertFalse(up.isExtension(".gxt"));
        assertFalse(up.isExtension("gxt"));
        assertFalse(up.isExtension("tx"));
        assertFalse(up.isExtension("gug"));
        assertFalse(up.isExtension(""));
    }

    private void testAll() {
        for (String tt : TO_TEST) {
            testOnePath(tt);
        }
    }

    private void testOnePath(String pth) {
        UnixPath a = factory.fromString(pth);
        Path b = Paths.get(pth);
        comparePaths(a, b);
    }

    @Test
    public void testResolvePerversities() {
        ComponentUPath p = ComponentUPath.ofUnixPaths("x", "", "", "");
        assertEquals("x", p.toString());
        ComponentUPath p1 = p.resolve("").resolve("").resolve("");
        assertEquals("x", p1.toString());
        assertEquals("y", p.resolveSibling("y").toString());
        ComponentUPath p2 = p1.resolveSibling("");
        assertNull(p2);
    }

    Set<String> seen = new HashSet<>();

    private void comparePaths(UnixPath a, Path b) {

        if (a == null && b == null) {
            return;
        }
        if (seen.contains(b.toString())) {
            return;
        }
        seen.add(b.toString());
//        System.out.println("TEST " + b + " -> " + a);

        assertNotNull("A is null for " + b, a);
        assertNotNull("B is null for " + a, b);

        String aStr = a.toString();
        String bStr = b.toString();
        String msgBase = factory + ": '" + aStr + "' -> '" + bStr + "'";

        List<UnixPath> apaths = new ArrayList<>();
        List<Path> bpaths = new ArrayList<>();
        List<String> anames = new ArrayList<>();
        List<String> bnames = new ArrayList<>();

        for (Path p : a) {
            assertTrue(msgBase, p instanceof UnixPath);
            apaths.add((UnixPath) p);
            anames.add(p.toString());
        }
        for (Path p : b) {
            bpaths.add(p);
            bnames.add(p.toString());
        }
        assertEquals("Exp null filename match got '" + a.getFileName() + "' expected  '" + b.getFileName()
                + "' in " + msgBase + " for " + a.getClass().getName(), (a.getFileName() == null), b.getFileName() == null);
        if (a.getFileName() != null) {
            assertTrue(msgBase, a.toString().endsWith(
                    a.getFileName().toString()));
            assertEquals(msgBase, b.isAbsolute(), a.isAbsolute());
            assertEquals(msgBase, b.getNameCount(), a.getNameCount());
            assertNotNull(msgBase + " - " + a.getClass().getName()
                    + " a null file name", a.getFileName());
            assertNotNull(msgBase + " - " + b.getClass().getName()
                    + " has null file name", b.getFileName());
            assertEquals(msgBase, b.getFileName().toString(),
                    a.getFileName().toString());
        }
        for (int i = 0; i < a.getNameCount(); i++) {
            assertEquals(msgBase, b.getName(i).toString(), a.getName(i).toString());
        }
        assertEquals(msgBase + " " + bnames + " vs. " + anames, bnames.size(), anames.size());
        assertEquals(msgBase + " " + bnames + " vs. " + anames, bnames, anames);
        assertEquals(a.toString(), b.toString());

        if (!a.isAbsolute()) {
            UnixPath abA = a.toAbsolutePath();
            assertNotSame(msgBase, a, abA);
            Path abB = b.toAbsolutePath();
            assertNotSame(msgBase, b, abB);
            assertTrue(msgBase, abA.isAbsolute());
            assertTrue(msgBase, abB.isAbsolute());
            String gotTweaked = abB.toString();
            if (a instanceof ComponentUPath && a.getNameCount() == 1 && !a.isAbsolute() && a.getFileName() != null && "".equals(a.getFileName().toString())) {
                // UnixPath for "" as an absolute path retains its '/' so you
                // get a path /com/foo/ - this is a quirk we're not interested
                // in replicating, but for all other cases it will be consistent
                // and is worth testing
                gotTweaked += "/";
            }
            assertEquals(msgBase + " expect abs '" + abA + "' got abs '"
                    + gotTweaked + "'", abA.toString(), gotTweaked);
        }
        if (a.getNameCount() >= 1) {
            comparePaths(a.getParent(), b.getParent());
            UnixPath aSib = a.resolveSibling("x/y/z");
            Path bSib = b.resolveSibling("x/y/z");
            assertEquals("Sibling " + msgBase, bSib.toString(), aSib.toString());
        }
        if (a.getNameCount() > 1) {
            UnixPath asub = a.subpath(1, a.getNameCount());
            Path bsub = b.subpath(1, b.getNameCount());
            assertEquals(asub == null, bsub == null);
            comparePaths(asub, bsub);
        }
        UnixPath anorm = a.normalize();
        if (anorm != a) {
            Path bnorm = b.normalize();
            assertEquals(msgBase + " normalizes differently", bnorm.toString(), anorm.toString());
            comparePaths(anorm, bnorm);
        }
    }

    interface UPathFactory {

        UnixPath fromPath(Path pth);

        UnixPath fromString(String pth);
    }

    static class CompFactory implements UPathFactory {

        @Override
        public UnixPath fromPath(Path pth) {
            return ComponentUPath.of(pth);
        }

        @Override
        public UnixPath fromString(String pth) {
            return ComponentUPath.of(pth);
        }

        public String toString() {
            return "ComponentUPath ";
        }
    }

    static class WrapFactory implements UPathFactory {

        @Override
        public UnixPath fromPath(Path pth) {
            return WrappedUPath.of(pth);
        }

        @Override
        public UnixPath fromString(String pth) {
            return new WrappedUPath(Paths.get(pth));
        }

        @Override
        public String toString() {
            return "WrappedUPath ";
        }
    }
}
