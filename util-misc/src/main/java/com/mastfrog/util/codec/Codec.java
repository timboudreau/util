package com.mastfrog.util.codec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Wrapper for ObjectMapper, or BSON codec or whatever. Subset of Jackson's
 * ObjectMapper's API.
 * <p>
 * A note about streams: A Codec instance is generally a wrapper for some other
 * encoding / decoding library. Some such libraries will close the stream after
 * reading/writing one object. If that is not what you want, most of them can be
 * configured not to do that - configure this behavior there.
 * </p>
 *
 * @author Tim Boudreau
 */
public interface Codec {

    /**
     * Get a codec that reads and writes serialized objects; note that methods
     * that read and write Strings use default Base64 encoding.
     *
     * @return A codec
     */
    public static Codec javaSerialization() {
        return SerializationCodec.INSTANCE;
    }

    /**
     * Create a Codec instance that uses Java serialization under the hood, and
     * the passed string encoding for to/from string methods.
     *
     * @param closesStreams If true, any stream wrapped by this codec will close
     * the underlying stream when it is closed; if false, the underlying stream
     * will be left open.
     * @param stringEncoding The encoding to use for string conversions
     * @return A codec
     */
    public static Codec javaSerialization(boolean closesStreams, StringEncoding stringEncoding) {
        if (stringEncoding == null) {
            // Not bothering to add a dependency on util-preconditions here
            throw new IllegalArgumentException("stringEncoding is null");
        }
        return new SerializationCodec(closesStreams, stringEncoding);
    }

    /**
     * Read a value from a string, instantiating an instance of the passed type.
     * Some codecs use a different string and binary representation, such as the
     * serialization codec.
     *
     * @param <T> A type
     * @param value The encoded string to decode and deserialize
     * @param type The type
     * @return An instance of the type
     * @throws IOException if something goes wrong
     */
    default <T> T readValue(String value, Class<T> type) throws IOException {
        return readValue(value.getBytes(UTF_8), type);
    }

    /**
     * Read a value from an input stream.
     *
     * @param <T> The type
     * @param byteBufInputStream The stream
     * @param type The type
     * @return An object
     * @throws IOException If something goes wrong
     */
    <T> T readValue(InputStream byteBufInputStream, Class<T> type) throws IOException;

    /**
     * Convert the passed object into a byte array.
     *
     * @param <T> The type
     * @param object An object of the type
     * @return a byte array
     * @throws IOException If something goes wrong
     */
    <T> byte[] writeValueAsBytes(T object) throws IOException;

    /**
     * Serialize to a string.
     *
     * @param <T> The type
     * @param object The object
     * @return A string
     * @throws IOException If something goes wrong
     */
    default <T> String writeValueAsString(T object) throws IOException {
        return new String(writeValueAsBytes(object), UTF_8);
    }

    /**
     * Write a value to an output stream.
     *
     * @param <T> The type
     * @param object An object
     * @param out The output
     * @throws IOException if something goes wrong
     */
    default <T> void writeValue(T object, OutputStream out) throws IOException {
        out.write(writeValueAsBytes(object));
    }

    /**
     * Read a value from a byte array.
     *
     * @param <T> The type
     * @param bytes The bytes to read
     * @param type The type to instantiate
     * @return An object
     * @throws IOException if something goes wrong
     */
    default <T> T readValue(byte[] bytes, Class<T> type) throws IOException {
        return readValue(new ByteArrayInputStream(bytes), type);
    }

    /**
     * Write a value to a byte buffer.
     *
     * @param <T> The type
     * @param object The object
     * @param into The buffer
     * @throws IOException if something goes wrong
     */
    default <T> void writeValue(T object, ByteBuffer into) throws IOException {
        into.put(ByteBuffer.wrap(writeValueAsBytes(object)));
    }
}
