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
package com.mastfrog.util.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class SerializationCodecTest {

    private Random rnd;

    @ParameterizedTest
    @EnumSource(StringEncoding.class)
    public void testCodec(StringEncoding enc) throws Exception {
        assertTrue(true);
        SerializationCodec codec = new SerializationCodec(false, enc);
        for (int i = 0; i < 5; i++) {

            String string = "Just a simple, short string.  Yay! @!^(@!@$^$&√®œ∑¥´ˆˆ Qgww~%></";
            String encoded = codec.writeValueAsString(string);
            
            System.out.println(enc + ": " + encoded);
            
            String decoded = codec.readValue(encoded, String.class);
            assertEquals(string, decoded,
                    "Could not correctly deserialize a string with " + enc);

            ThingWithStuff thing = new ThingWithStuff(rnd);
            // sanity check that serialization works at all
            ThingWithStuff sanityCheck = serDeser(thing);
            assertNotSame(thing, sanityCheck);
            assertEquals(thing, sanityCheck);

            String asString = codec.writeValueAsString(thing);

            ThingWithStuff dec = codec.readValue(asString, ThingWithStuff.class);

            assertNotSame(thing, dec, enc.name());
            assertEquals(thing, dec, enc + ": Encoding '" + asString
                    + "' did not result in equal object");

            byte[] bytes = codec.writeValueAsBytes(thing);

            ThingWithStuff decBytes = codec.readValue(bytes, ThingWithStuff.class);
            assertEquals(thing, decBytes, enc + ": Byte encoding to " + bytes.length
                    + " bytes did not result in equal object");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream wrapOut = enc.newEncoder().wrap(baos);
            try ( ObjectOutputStream oout = new ObjectOutputStream(wrapOut)) {
                oout.writeObject(thing);
            } catch (IOException ioe) {
                throw new AssertionError("IOE from " + wrapOut + " for " + enc, ioe);
            }
            InputStream wrapped = enc.newDecoder().wrap(new ByteArrayInputStream(baos.toByteArray()));
            try ( ObjectInputStream oin = new ObjectInputStream(wrapped)) {
                ThingWithStuff fromStream = (ThingWithStuff) oin.readObject();
                assertEquals(thing, fromStream, "Stream decoding failed for " + new String(baos.toByteArray(), UTF_8));
            } catch (IOException ioe) {
                throw new AssertionError("IOE reading " + wrapped + " for " + enc, ioe);
            }
        }
    }

    private ThingWithStuff serDeser(ThingWithStuff orig) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try ( ObjectOutputStream oout = new ObjectOutputStream(baos)) {
            oout.writeObject(orig);
        }
        try ( ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            return (ThingWithStuff) oin.readObject();
        }
    }

    @BeforeEach
    public void setup() {
        rnd = new Random(193);
    }

    public static final class ThingWithStuff implements Serializable {

        public int someInt;
        public String someString;
        public List<String> someMoreStrings;
        public OtherThing otherThing;
        public byte oneByte;
        public short someShort;
        public long[] longs;

        public ThingWithStuff(int someInt, String someString, List<String> someMoreStrings, OtherThing someBytes, byte oneByte, short someShort, long[] longs) {
            this.someInt = someInt;
            this.someString = someString;
            this.someMoreStrings = someMoreStrings;
            this.otherThing = someBytes;
            this.oneByte = oneByte;
            this.someShort = someShort;
            this.longs = longs;
        }

        public ThingWithStuff() {
        }

        public ThingWithStuff(Random rnd) {
            this.someInt = rnd.nextInt();
            this.someString = randomString(rnd, 7);
            this.someMoreStrings = randomStrings(rnd);
            this.otherThing = new OtherThing(rnd);
            this.oneByte = (byte) rnd.nextInt();
            this.someShort = (short) rnd.nextInt();
            this.longs = randomLongs(rnd, rnd.nextInt(5) + 2);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + this.someInt;
            hash = 59 * hash + Objects.hashCode(this.someString);
            hash = 59 * hash + Objects.hashCode(this.someMoreStrings);
            hash = 59 * hash + Objects.hashCode(this.otherThing);
            hash = 59 * hash + this.oneByte;
            hash = 59 * hash + this.someShort;
            hash = 59 * hash + Arrays.hashCode(this.longs);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ThingWithStuff other = (ThingWithStuff) obj;
            if (this.someInt != other.someInt) {
                return false;
            }
            if (this.oneByte != other.oneByte) {
                return false;
            }
            if (this.someShort != other.someShort) {
                return false;
            }
            if (!Objects.equals(this.someString, other.someString)) {
                return false;
            }
            if (!Objects.equals(this.someMoreStrings, other.someMoreStrings)) {
                return false;
            }
            if (!Objects.equals(this.otherThing, other.otherThing)) {
                return false;
            }
            return Arrays.equals(this.longs, other.longs);
        }

        @Override
        public String toString() {
            return "ThingWithStuff{" + "someInt=" + someInt + ", someString="
                    + someString + ", someMoreStrings=" + someMoreStrings
                    + ", someBytes=" + otherThing + ", oneByte=" + oneByte
                    + ", someShort=" + someShort + ", longs="
                    + Arrays.toString(longs) + '}';
        }
    }

    public static final class OtherThing implements Serializable {

        public byte[] someBytes;
        public Date someDate;

        public OtherThing() {

        }

        public OtherThing(byte[] bytes, Date date) {
            this.someBytes = bytes;
            this.someDate = date;
        }

        public OtherThing(Random rnd) {
            this.someDate = new Date(Math.abs(rnd.nextLong() % 1000000));
            this.someBytes = randomBytes(rnd, rnd.nextInt(24) + 24);
        }

        @Override
        public String toString() {
            return "OtherThing{" + "someBytes="
                    + Arrays.toString(someBytes) + ", someDate="
                    + someDate + '}';
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Arrays.hashCode(this.someBytes);
            hash = 97 * hash + Objects.hashCode(this.someDate);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final OtherThing other = (OtherThing) obj;
            if (!Arrays.equals(this.someBytes, other.someBytes)) {
                return false;
            }
            return Objects.equals(this.someDate, other.someDate);
        }
    }

    private static byte[] randomBytes(Random rnd, int len) {
        byte[] result = new byte[len];
        rnd.nextBytes(result);
        return result;
    }

    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private static String randomString(Random rnd, int chars) {
        char[] c = new char[chars];
        for (int i = 0; i < chars; i++) {
            c[i] = ALPHANUM[rnd.nextInt(ALPHANUM.length)];
        }
        return new String(c);
    }

    private static long[] randomLongs(Random rnd, int count) {
        long[] result = new long[count];
        for (int i = 0; i < count; i++) {
            result[i] = rnd.nextLong();
        }
        return result;
    }

    private static List<String> randomStrings(Random rnd) {
        int count = rnd.nextInt(5) + 2;
        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(randomString(rnd, rnd.nextInt(10) + 3));
        }
        return result;
    }
}
