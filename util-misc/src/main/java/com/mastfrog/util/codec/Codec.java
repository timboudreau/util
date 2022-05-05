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
 * A note about streams:  A Codec instance is generally a wrapper for some
 * other encoding / decoding library.  Some such libraries will close the
 * stream after reading/writing one object.  If that is not what you want,
 * most of them can be configured not to do that - configure this behavior
 * there.
 * </p>
 *
 * @author Tim Boudreau
 */
public interface Codec {

    /**
     * Get a codec that reads and writes serialized objects.
     *
     * @return A codec
     */
    public static Codec javaSerialization() {
        return SerializationCodec.INSTANCE;
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
