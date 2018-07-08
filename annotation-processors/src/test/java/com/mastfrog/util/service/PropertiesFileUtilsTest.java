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
package com.mastfrog.util.service;

import com.mastfrog.util.Strings;
import com.mastfrog.util.collections.CollectionUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class PropertiesFileUtilsTest {

    @Test
    public void testReadWrite() throws Throwable {
        testOne(props("hello", "world", "this", "is", "simple", "and", "should", "work"));

        testOne(props("I've", "got", "elves'", "feet", "and", "I'm", "quite", "pissed"));

        testOne(props("url", "http://localhost:8080", "form", "2:80"));

        testOne(props("hoog", "\"I have an = tatooed on my head's crown\"", "form", "::2:80"));
    }

    private Properties props(String... kvs) {
        Properties result = new Properties();
        for (int i = 0; i < kvs.length; i += 2) {
            result.setProperty(kvs[i], kvs[i + 1]);
        }
        return result;
    }

    private void testOne(Properties props) throws Throwable {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PropertiesFileUtils.savePropertiesFile(props, out, "hey", true);

        Properties nue = new Properties();
        nue.load(new ByteArrayInputStream(out.toByteArray()));

        if (!props.stringPropertyNames().equals(nue.stringPropertyNames())) {
            Set<String> d = CollectionUtils.disjunction(props.stringPropertyNames(), nue.stringPropertyNames());
            fail("Result does not contain the same keys: " + Strings.join(',', d));
        }

        for (String s : props.stringPropertyNames()) {
            String expect = props.getProperty(s);
            String got = nue.getProperty(s);

            assertEquals(s + "\t" + expect + "\t\t" + got, expect, got);
        }
    }

}
