package com.mastfrog.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wrapper for ObjectMapper, or BSON codec or whatever.
 *
 * @author Tim Boudreau
 */
public interface Codec {

    public <T> String writeValueAsString(T object) throws IOException;

    public <T> void writeValue(T object, OutputStream out) throws IOException;

    public <T> byte[] writeValueAsBytes(T object) throws IOException;

    public <T> T readValue(InputStream byteBufInputStream, Class<T> type) throws IOException;
}
