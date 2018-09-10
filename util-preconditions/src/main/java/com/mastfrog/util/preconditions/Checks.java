/*
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
package com.mastfrog.util.preconditions;

import java.io.File;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Code sanity checks, to simplify things like null checks. Use these where
 * applicable instead of directly throwing exceptions. That way, expensive tests
 * can be switched to use assertions for production.
 *
 * @author Tim Boudreau
 */
public final class Checks {

    static boolean disabled = Boolean.getBoolean("checksDisabled");

    private Checks() {
    }

    public static long canCastToInt(String name, long value) {
        if (disabled) {
            return value;
        }
        if (value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " too large for an integer: " + value);
        }
        if (value < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(name + " too small for an integer: " + value);
        }
        return value;
    }

    public static void atLeastOneNotNull(String msg, Object... objects) {
        if (disabled) {
            return;
        }
        boolean foundNonNull = false;
        for (Object o : objects) {
            if (o != null) {
                foundNonNull = true;
                break;
            }
        }
        if (!foundNonNull) {
            throw new NullArgumentException(msg);
        }
    }

    /**
     * Determine that the passed parameter is not null
     *
     * @param name The name of the parameter
     * @param val The value of the parameter
     * @throws NullPointerException if the name is null
     * @throws NullArgumentException if the value is null
     */
    public static <T> T notNull(String name, T val) {
        if (disabled) {
            return val;
        }
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        if (val == null) {
            throw new NullArgumentException(name + " is null");
        }
        return val;
    }

    /**
     * Determine that the passed argument is not a negative number
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static <T extends Number> T nonNegative(String name, T val) {
        if (disabled) {
            return val;
        }
        notNull("name", name);
        if (val.longValue() < 0) {
            throw new IllegalArgumentException(name + " cannot be a negative number but is " + val);
        }
        return val;
    }

    /**
     * Determine that the passed argument is not a negative number
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static int nonNegative(String name, int val) {
        if (disabled) {
            return val;
        }
        notNull("name", name);
        if (val < 0) {
            throw new IllegalArgumentException(name + " cannot be a negative number but is " + val);
        }
        return val;
    }

    /**
     * Determine that the passed argument is not a negative number.
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static long nonNegative(String name, long val) {
        if (disabled) {
            return val;
        }
        notNull("name", name);
        if (val < 0) {
            throw new IllegalArgumentException(name + " cannot be a negative number but is " + val);
        }
        return val;
    }

    /**
     * Determine that the passed argument is greater than one.
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static <T extends Number> T greaterThanOne(String name, T val) {
        if (disabled) {
            return val;
        }
        notNull("name", name);
        if (val.longValue() < 1) {
            throw new IllegalArgumentException(name + " cannot be < 1 but is " + val);
        }
        return val;
    }

    /**
     * Determine that the passed argument is greater than one.
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static int greaterThanOne(String name, int val) {
        if (disabled) {
            return val;
        }
        notNull("name", name);
        if (val < 1) {
            throw new IllegalArgumentException(name + " cannot be < 1 but is " + val);
        }
        return val;
    }

    /**
     * Determine that the passed argument is greater than one.
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static long greaterThanOne(String name, long val) {
        if (disabled) {
            return val;
        }
        notNull("name", name);
        if (val < 1L) {
            throw new IllegalArgumentException(name + " cannot be < 1 but is " + val);
        }
        return val;
    }

    /**
     * Determine that the passed argument &gt;=0.
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static <T extends Number> T greaterThanZero(String name, T val) {
        if (disabled) {
            return val;
        }
        notNull("name", name);
        if (val.longValue() < 1 || val.doubleValue() < 1) {
            throw new IllegalArgumentException(name + " cannot be < 1 but is " + val);
        }
        return val;
    }

    /**
     * Determine that the passed argument &gt;=0.
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static int greaterThanZero(String name, int val) {
        if (disabled) {
            return val;
        }
        notNull("name", name);
        if (val < 1) {
            throw new IllegalArgumentException(name + " cannot be < 1 but is " + val);
        }
        return val;
    }

    /**
     * Determine that the passed argument &gt;=1.
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static long greaterThanZero(String name, long val) {
        if (disabled) {
            return val;
        }
        notNull("name", name);
        if (val < 1) {
            throw new IllegalArgumentException(name + " cannot be < 1 but is " + val);
        }
        return val;
    }

    /**
     * Determine that the passed array argument
     * <ul>
     * <li>is an array</li>
     * <li>is not null</li>
     * <li>has a length > 0</li>
     *
     * @param name The parameter name for use in an exception
     * @param array An array
     * @throws IllegalArgumentException if array is not an array
     * @throws IllegalArgumentException
     */
    public static <T> T notEmptyOrNull(String name, T array) {
        if (disabled) {
            return array;
        }
        notNull(name, array);
        Class<?> arrType = array.getClass();
        if (!arrType.isArray()) {
            throw new IllegalArgumentException("Not an array: " + array);
        }
        if (Array.getLength(array) == 0) {
            throw new IllegalArgumentException(name + " has 0 length");
        }
        return array;
    }

    /**
     * Verify that an array does not contain null elements, throwing an
     * exception if it does.
     * <i>Do not use for primitive array types - they cannot contain nulls
     * anyway</i>
     *
     * @param name The name of the parameter
     * @param arr An array of some sort
     * @throws IllegalArgumentException if the array has null elements, or if
     * array is an array of primitive types (these cannot contain null values
     * and do not need such checking)
     * @throws NullArgumentException if the passed array is null
     */
    @SafeVarargs
    public static <T> T[] noNullElements(String name, T... arr) {
        if (disabled) {
            return arr;
        }
        notNull(name, arr);
        Class<?> arrType = arr.getClass();
        if (arrType.getComponentType().isPrimitive()) {
            throw new IllegalArgumentException("Null checks not "
                    + "needed for primitive arrays such as "
                    + name + " (" + arrType.getComponentType() + ")");

        }
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == null) {
                throw new NullArgumentException("Null element at " + i
                        + " in " + name + " (" + Arrays.asList(arr) + ")");
            }
        }
        return arr;
    }

    /**
     * Throws an exception if a parameter is equal to zero.
     *
     * @param name The name of the parameter
     * @param val A number
     * @throws IllegalArgumentException if the value of the number equals 0
     */
    public static <T extends Number> T nonZero(String name, T val) {
        if (disabled) {
            return val;
        }
        notNull(name, val);
        if (val.longValue() == 0) {
            throw new IllegalStateException(name + " should not be 0");
        }
        return val;
    }

    /**
     * Throws an exception if a parameter is equal to zero.
     *
     * @param name The name of the parameter
     * @param val A number
     * @throws IllegalArgumentException if the value of the number equals 0
     */
    public static int nonZero(String name, int val) {
        if (disabled) {
            return val;
        }
        if (val == 0) {
            throw new IllegalStateException(name + " should not be 0");
        }
        return val;
    }

    /**
     * Throws an exception if a parameter is equal to zero.
     *
     * @param name The name of the parameter
     * @param val A number
     * @throws IllegalArgumentException if the value of the number equals 0
     */
    public static long nonZero(String name, long val) {
        if (disabled) {
            return val;
        }
        if (val == 0) {
            throw new IllegalStateException(name + " should not be 0");
        }
        return val;
    }

    /**
     * Throws an exception if the passed string is empty
     *
     * @param name The name of the string used in the exception
     * @param value A string value
     * @throws IllegalArgumentException if the String is empty,
     * <i>but not if it is null</i>
     */
    public static <T extends CharSequence> T notEmpty(String name, T value) {
        if (disabled) {
            return value;
        }
        notNull("name", name);
        if (value instanceof String && ((String) value).isEmpty()) {
            throw new IllegalArgumentException("String " + name
                    + " cannot be 0-length");
        }
        if (value != null && value.length() == 0) {
            throw new IllegalArgumentException("String " + name
                    + " cannot be 0-length");
        }
        return value;
    }

    public static void notEmpty(String name, Collection<?> collection) {
        if (disabled) {
            return;
        }
        if (collection != null && collection.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be an empty "
                    + "collection (" + collection + ")");
        }
    }

    /**
     * Throws an exception if the passed string is null or empty
     *
     * @param name The parameter name for use in the exception message
     * @param value The value
     * @throws IllegalArgumentException if the value is null or "".equals(value)
     * @throws NullArgumentException if the passed value is null
     */
    public static <T extends CharSequence> T notNullOrEmpty(String name, T value) {
        if (disabled) {
            return value;
        }
        notNull(name, value);
        return notEmpty(name, value);
    }

    /**
     * Throws an exception if the passed string contains any of the passed
     * characters
     *
     * @param name The parameter name for use in the exception message
     * @param value The value (may be null - use notNull(name, value) for null
     * checks
     * @param chars An array of characters
     * @throws IllegalArgumentException if the value contains any of the passed
     * characters, or if the passed character array length is 0
     * @throws NullArgumentException if the passed char array is null
     */
    public static void mayNotContain(String name, CharSequence value, char... chars) {
        if (disabled) {
            return;
        }
        notNull("chars", chars);
        if (value == null) {
            return;
        }
        if (chars.length == 0) {
            throw new IllegalArgumentException("0 length list of characters");
        }
        if (chars.length == 1) {
            int index = indexIn(value, chars[0]);
            if (index > 0) {
                throw new IllegalArgumentException("Illegal character at "
                        + index + " in " + value);
            }
        } else {
            for (char c : chars) {
                int index = indexIn(value, chars[0]);
                if (index > 0) {
                    throw new IllegalArgumentException("Illegal character " + c
                            + "at " + index + " in " + value);
                }
            }
        }
    }

    private static int indexIn(CharSequence seq, char c) {
        if (seq instanceof String) {
            return ((String) seq).indexOf(c);
        } else if (seq instanceof StringBuilder) {
            return ((StringBuilder) seq).indexOf("" + c);
        } else {
            int max = seq.length();
            for (int i = 0; i < max; i++) {
                if (c == seq.charAt(i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Throws an exception if the passed value does not contain at least one of
     * the passed character
     *
     * @param name The parameter name for constructing the exception message
     * @param value A value
     * @param c A character which must be present
     */
    public static void mustContain(String name, CharSequence value, char c) {
        if (disabled) {
            return;
        }
        if (value != null && indexIn(value, c) < 0) {
            throw new IllegalArgumentException(name + " must contain a '" + c + "' character but does not (" + value + ")");
        }
    }

    /**
     * Throws an exception if the passed set contains duplicate elements.
     * <b>Note:</b> do not use this method on very large collections or lazily
     * resolved collections, as it will trigger resolving and iterating the
     * entire collection.
     *
     * @param name The parameter name for constructing the exception message
     * @param collection A collection of something
     */
    public static void noDuplicates(String name, Collection<?> collection) {
        if (disabled) {
            return;
        }
        if (!(collection instanceof Set)) {
            Set<Object> nue = new HashSet<>(collection);
            if (nue.size() != collection.size()) {
                throw new IllegalArgumentException(name + " contains duplicate entries (" + collection + ")");
            }
        }
    }

    /**
     * Throws an exception if the passed string starts with the passed character
     *
     * @param name The parameter name for constructing the exception message
     * @param value a string or equivalent. May be null, use notNull() for null
     * checks.
     * @throws IllegalArgumentException if the passed value starts with the
     * passed character
     */
    public static void mayNotStartWith(String name, CharSequence value, char c) {
        if (disabled) {
            return;
        }
        if (value != null && value.length() > 0 && c == value.charAt(0)) {
            throw new IllegalArgumentException(name + " may not start with a '" + c + "' character (" + value + ")");
        }
    }

    /**
     * Determine if an object is an instance of a given type
     *
     * @param name The parameter name for constructing the exception message
     * @param type The type sought
     * @param value The object
     * @throws ClassCastException if the passed object is of the wrong type
     */
    public static void isInstance(String name, Class<?> type, Object value) {
        if (disabled) {
            return;
        }
        if (value != null) {
            if (!type.isInstance(value)) {
                throw new ClassCastException(value
                        + " is not an instance of " + type.getName() + " (" + value.getClass().getName() + ")");
            }
        }
    }

    /**
     * Check that the given argument is of the given length, throw an
     * IllegalArgumentException if that is not the case.
     *
     * @param name The parameter name for constructing the exception message
     * @param length The required length of the given value
     * @param value The value to check the length of
     * @throws IllegalArgumentException if the length of the passed in value is
     * not equal to the specified required length
     * @throws NullPointerException if the name is null
     * @throws NullArgumentException if the value is null
     */
    public static void isOfLength(String name, int length, Object[] value) {
        if (disabled) {
            return;
        }
        notNull(name, value);
        if (value.length != length) {
            throw new IllegalArgumentException("value " + Objects.toString(value) + " does not have the required arguments of " + length);
        }
    }

    /**
     * Test that file exists and is a file not a folder.
     *
     * @param file The file
     */
    public static void fileExists(File file) {
        if (disabled) {
            return;
        }
        notNull("file", file);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException(file + " does not exist or is not a regular file");
        }
    }

    /**
     * Test that file exists and is a file not a folder.
     *
     * @param file The file
     * @param paramName The name of the method parameter
     */
    public static File fileExists(String paramName, File file) {
        if (disabled) {
            return file;
        }
        notNull(paramName, file);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException(paramName + " does not exist or is not a regular file: " + file);
        }
        return file;
    }

    /**
     * Test that a file exists and is a folder.
     *
     * @param file The file
     */
    public static void folderExists(File file) {
        if (disabled) {
            return;
        }
        notNull("file", file);
        if (!file.exists() || !file.isDirectory()) {
            throw new IllegalArgumentException(file + " does not exist or is not a regular file");
        }
    }

    /**
     * Test that a file exists and is a folder.
     *
     * @param file The file
     */
    public static File folderExists(String paramName, File file) {
        if (disabled) {
            return file;
        }
        notNull(paramName, file);
        Path pth = file.toPath();
        if (!file.exists()) {
            throw new IllegalArgumentException(paramName + " does not exist: " + file);
        }
        if (!file.isDirectory()) {
            throw new IllegalArgumentException(paramName + " is not a directory: " + file);
        }
        return file;
    }

    /**
     * Test that a file exists and the current user has read permission.
     *
     * @param file A file
     */
    public static void readable(File file) {
        if (disabled) {
            return;
        }
        fileExists(file);
        if (!file.canRead()) {
            throw new IllegalArgumentException("Read permission missing on " + file);
        }
    }

    /**
     * Test that a file exists and the current user has read permission.
     *
     * @param file A file
     */
    public static File readable(String paramName, File file) {
        if (disabled) {
            return file;
        }
        fileExists(paramName, file);
        if (!file.canRead()) {
            throw new IllegalArgumentException(paramName + " exists and is a file but is not readable: " + file);
        }
        return file;
    }

    /**
     * Test that a file exists and the current user has read permission.
     *
     * @param file A file
     */
    public static File readableAndNonZeroLength(String paramName, File file) {
        if (disabled) {
            return file;
        }
        if (readable(paramName, file).length() == 0) {
            throw new IllegalArgumentException(paramName + " exists and is a file but is not readable: " + file);
        }
        return file;
    }

    /**
     * Test that a character sequence is encodable in the passe charset.
     *
     * @param seq A string
     * @param charset A character set
     */
    public static void encodable(CharSequence seq, Charset charset) {
        if (disabled) {
            return;
        }
        notNull("seq", seq);
        notNull("charset", charset);
        CharsetEncoder encoder = charset.newEncoder();
        if (!encoder.canEncode(seq)) {
            if (seq.length() > 30) {
                seq = seq.subSequence(0, 30) + "...";
            }
            throw new IllegalArgumentException("Cannot be encoded in " + charset.name() + " '" + seq + "'");
        }
    }

}
