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
package com.mastfrog.util.fileformat;

import com.mastfrog.util.fileformat.SimpleJSON;
import static java.nio.charset.StandardCharsets.UTF_16;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class SimpleJSONTest {

    private static final ZonedDateTime WHEN = ZonedDateTime.parse("2018-07-04T20:37:19.914Z");
    private static final Duration dur = Duration.ofDays(483).plus(Duration.ofHours(21)).plus(Duration.ofMinutes(3)).plus(Duration.ofSeconds(12));

    @Test
    public void testSimpleTypeSerialization() {
        assertEquals("null", SimpleJSON.stringify(null));
        assertEquals("1", SimpleJSON.stringify(1));
        assertEquals("23", SimpleJSON.stringify(23));
        assertEquals("42.5", SimpleJSON.stringify(42.5));
        assertEquals("true", SimpleJSON.stringify(true));
        assertEquals("false", SimpleJSON.stringify(false));
        assertEquals("\"foo\"", SimpleJSON.stringify("foo"));
        assertEquals("\"2018-07-04T20:37:19.914Z\"", SimpleJSON.stringify(WHEN));
        assertEquals("\"PT11613H3M12S\"", SimpleJSON.stringify(dur));
        assertEquals("\"Fw80e+zTOYI=\"", SimpleJSON.stringify(new byte[]{23, 15, 52, 123, -20, -45, 57, -126}));
        assertEquals(Long.MAX_VALUE, Long.parseLong(SimpleJSON.stringify(Long.MAX_VALUE)));
        assertEquals(Long.MIN_VALUE, Long.parseLong(SimpleJSON.stringify(Long.MIN_VALUE)));
        assertEquals(Double.MAX_VALUE, Double.parseDouble(SimpleJSON.stringify(Double.MAX_VALUE)), 0.00001);
        assertEquals(Double.MIN_VALUE, Double.parseDouble(SimpleJSON.stringify(Double.MIN_VALUE)), 0.00001);
    }

    @Test
    public void testStringEscaping() {
        assertEquals("\"He said \\\"Huh?\\\" and\\ttabbed\\n\"", SimpleJSON.stringify("He said \"Huh?\" and\ttabbed\n"));
    }

    @Test
    public void testArraySerialization() {
        assertEquals("[1,2,3,4,5]", SimpleJSON.stringify(new int[]{1, 2, 3, 4, 5}));
        assertEquals("[-1,-2,-3,-4,-5]", SimpleJSON.stringify(new int[]{-1, -2, -3, -4, -5}));
        assertEquals("[231.214,2.3,-3.0,4.0,5.0]", SimpleJSON.stringify(new double[]{231.214, 2.3, -3, 4, 5}));
    }

    @Test
    public void testMapSerialization() {
        Map<String, Object> m = new HashMap<>();
        m.put("hey", "you");
        m.put("foo", 23);
        m.put("stuff", Arrays.asList("a", "b", "c", "d"));
        Map<String, Object> m2 = new HashMap<>();
        m2.put("nested", true);
        Map<String, Object> m3 = new HashMap<>();
        m3.put("nestedList", Arrays.asList("this", "is", "a", "nested", "list", "tabs\tare\tcool\n"));
        m3.put("hoo", "hah");
        m3.put("numbers", new short[]{(short) 1, (short) 3, (short) 5});
        m2.put("deeper", m3);
        m2.put("hey", 124204.0202013D);
        m.put("nesty", m2);
        m.put("when", WHEN);
        m3.put("howLong", dur);
        m3.put("bytes", "Hello world".getBytes(UTF_16));
        String ser = SimpleJSON.stringify(m, SimpleJSON.Style.PRETTY);
        String expect = "{\n"
                + "\"foo\" : 23, \n"
                + "\"nesty\" : {\n"
                + "  \"deeper\" : {\n"
                + "    \"nestedList\" : [\n"
                + "      \"this\", \n"
                + "      \"is\", \n"
                + "      \"a\", \n"
                + "      \"nested\", \n"
                + "      \"list\", \n"
                + "      \"tabs\\tare\\tcool\\n\"\n"
                + "      ], \n"
                + "    \"bytes\" : \"/v8ASABlAGwAbABvACAAdwBvAHIAbABk\", \n"
                + "    \"numbers\" : [\n"
                + "      1, \n"
                + "      3, \n"
                + "      5\n"
                + "      ], \n"
                + "    \"hoo\" : \"hah\", \n"
                + "    \"howLong\" : \"PT11613H3M12S\"}, \n"
                + "  \"nested\" : true, \n"
                + "  \"hey\" : 124204.0202013}, \n"
                + "\"hey\" : \"you\", \n"
                + "\"when\" : \"2018-07-04T20:37:19.914Z\", \n"
                + "\"stuff\" : [\n"
                + "  \"a\", \n"
                + "  \"b\", \n"
                + "  \"c\", \n"
                + "  \"d\"\n"
                + "  ]}";
        assertEquals(expect, ser);
    }

    @Test
    public void testReflection() throws Throwable {
        Thing1 t1 = new Thing1("isFoo", "skiddoo", 23, true, 57);
        String s = SimpleJSON.stringify(t1, SimpleJSON.Style.MINIFIED);
        assertEquals("{\"bar\":\"skiddoo\",\"baz\":23,\"foo\":\"isFoo\",\"quux\":true}", s);
        OtherThing ot = new OtherThing(t1, "hey");
        String s2 = SimpleJSON.stringify(new Object[]{ot}, SimpleJSON.Style.MINIFIED);
        assertEquals("[{\"thing1\":{\"bar\":\"skiddoo\",\"baz\":23,\"foo\":\"isFoo\",\"quux\":true},\"whatev\":\"hey\"}]", s2);
    }

    public static final class Thing1 {

        public final String foo;
        public final String bar;
        public final int baz;
        public final boolean quux;
        private final int cantSeeMe;

        public Thing1(String foo, String bar, int baz, boolean quux, int cantSeeMe) {
            this.foo = foo;
            this.bar = bar;
            this.baz = baz;
            this.quux = quux;
            this.cantSeeMe = cantSeeMe;
        }
    }

    public static final class OtherThing {
        public final Thing1 thing1;
        public final String whatev;

        public OtherThing(Thing1 thing1, String whatev) {
            this.thing1 = thing1;
            this.whatev = whatev;
        }

    }
}
