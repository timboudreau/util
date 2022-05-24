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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Enumeration;

/**
 *
 * @author Tim Boudreau
 */
public class M1X {

    public static int foo() throws IOException {
        int ct = 0;
        for (InputStream in : MetaInfLoader.loadAll("stuff/stuff.txt")) {
            ct++;
            System.out.println(ct + ". " + read(in));
        }
        System.out.println("COunt " + ct);
        return ct;
    }

    public static void tryStuff() throws IOException {
        tryEach("foo.txt");
        tryEach("META-INF/stuff/stuff.txt");
        tryEach("info/info.txt");
    }

    private static void tryEach(String n) throws IOException {
        System.out.println("\n------------ " + n + " ------------");
        System.out.println("FromClass: " + readOne(() -> fromClass(n)));
        System.out.println("FromLoadr: " + readOne(() -> fromClassLoader(n)));
        System.out.println("FromModul: " + readOne(() -> fromModule(n)));
        System.out.println("FromClURL: " + readOne(() -> fromClassLoaderURL(n)));
        System.out.println("FromCURLs: " + readOne(() -> fromClassLoaderURLs(n)));
    }

    private static InputStream fromClass(String name) {
        return M1X.class.getResourceAsStream(name);
    }

    private static InputStream fromClassLoader(String name) {
        return M1X.class.getClassLoader().getResourceAsStream(name);
    }

    private static InputStream fromModule(String name) throws IOException {
        return M1X.class.getModule().getResourceAsStream(name);
    }

    private static InputStream fromClassLoaderURL(String name) throws IOException {
        URL u = M1X.class.getResource(name);
        return u == null ? null : u.openStream();
    }

    private static InputStream fromClassLoaderURLs(String name) throws IOException {
        Enumeration<URL> u = M1X.class.getClassLoader().getResources(name);
        if (u.hasMoreElements()) {
            return u.nextElement().openStream();
        }
        return null;
    }

    interface TS {

        InputStream get() throws IOException;
    }

    private static String readOne(TS ts) throws IOException {
        return read(ts.get());
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) {
            return "-null-";
        }
        byte[] b = in.readAllBytes();
        return new String(b, UTF_8);
    }
}
