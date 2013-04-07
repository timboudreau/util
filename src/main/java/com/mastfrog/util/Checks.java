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
package com.mastfrog.util;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Code sanity checks, to simplify things like null checks.
 * Use these where applicable instead of directly throwing exceptions.
 * That way, expensive tests can be switched to use assertions for production.
 *
 * @author Tim Boudreau
 */
public final class Checks {

    private Checks() {
    }
    
    public static void canCastToInt (String name, long value) {
        if (value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " too large for an integer: " + value);
        }
        if (value < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(name + " too small for an integer: " + value);
        }
    }

    public static void atLeastOneNotNull (String msg, Object... objects) {
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
     * @param name The name of the parameter
     * @param val The value of the parameter
     * @throws NullPointerException if the name is null
     * @throws NullArgumentException if the value is null
     */
    public static void notNull(String name, Object val) {
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        if (val == null) {
            throw new NullArgumentException(name + " is null");
        }
    }

    /**
     * Determine that the passed argument is not a negative number
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static void nonNegative(String name, Number val) {
        notNull("name", name);
        notNull(name, val);
        if (val.longValue() < 0) {
            throw new IllegalArgumentException(name + " cannot be a negative number");
        }
    }

    /**
     * Determine that the passed array argument
     * <ul>
     * <li>is an array</li>
     * <li>is not null</li>
     * <li>has a length > 0</li>
     * @param name The parameter name for use in an exception
     * @param array An array
     * @throws IllegalArgumentException if array is not an array
     * @throws IllegalArgumentException
     */
    public static void notEmptyOrNull(String name, Object array) {
        notNull(name, array);
        Class<?> arrType = array.getClass();
        if (!arrType.isArray()) {
            throw new IllegalArgumentException("Not an array: " + array);
        }
        if (Array.getLength(array) == 0) {
            throw new IllegalArgumentException(name + " has 0 length");
        }
    }

    /**
     * Verify that an array does not contain null elements, throwing an exception
     * if it does.
     * <i>Do not use for primitive array types - they cannot contain nulls anyway</i>
     *
     * @param name The name of the parameter
     * @param arr An array of some sort
     * @throws IllegalArgumentException if the array has null elements,
     * or if array is an array of primitive
     * types (these cannot contain null values and do not need such checking)
     * @throws NullArgumentException if the passed array is null
     */
    public static void noNullElements(String name, Object[] arr) {
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
    }

    /**
     * Throws an exception if a parameter is equal to zero
     * @param name The name of the parameter
     * @param val A number
     * @throws IllegalArgumentException if the value of the number equals 0
     */
    public static void nonZero(String name, Number val) {
        notNull(name, val);
        if (val.longValue() == 0) {
            throw new IllegalStateException(name + " should not be 0");
        }
    }

    /**
     * Throws an exception if the passed string is empty
     * @param name The name of the string used in the exception
     * @param value A string value
     * @throws IllegalArgumentException if the String is empty,
     * <i>but not if it is null</i>
     */
    public static void notEmpty(String name, CharSequence value) {
        notNull("name", name);
        if (value != null && value.length() == 0) {
            throw new IllegalArgumentException("String " + name
                    + " cannot be 0-length");
        }
    }

    public static void notEmpty(String name, Collection<?> collection) {
        if (collection != null && collection.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be an empty "
                    + "collection (" + collection + ")");
        }
    }

    /**
     * Throws an exception if the passed string is null or empty
     * @param name The parameter name for use in the exception message
     * @param value The value
     * @throws IllegalArgumentException if the value is null or "".equals(value)
     * @throws NullArgumentException if the passed value is null
     */
    public static void notNullOrEmpty(String name, CharSequence value) {
        notNull(name, value);
        notEmpty(name, value);
    }

    /**
     * Throws an exception if the passed string contains any of the passed characters
     * @param name The parameter name for use in the exception message
     * @param value The value (may be null - use notNull(name, value) for null checks
     * @param chars An array of characters
     * @throws IllegalArgumentException if the value contains any of the passed characters,
     * or if the passed character array length is 0
     * @throws NullArgumentException if the passed char array is null
     */
    public static void mayNotContain(String name, CharSequence value, char... chars) {
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
     * Throws an exception if the passed value does not contain at least one
     * of the passed character
     * @param name The parameter name for constructing the exception message
     * @param value A value
     * @param c A character which must be present
     */
    public static void mustContain(String name, CharSequence value, char c) {
        if (value != null && indexIn(value, c) < 0) {
            throw new IllegalArgumentException(name + " must contain a '" + c + "' character but does not (" + value + ")");
        }
    }

    /**
     * Throws an exception if the passed set contains duplicate elements.
     * <b>Note:</b> do not use this method on very large collections or
     * lazily resolved collections, as it will trigger resolving and iterating
     * the entire collection.
     * @param name The parameter name for constructing the exception message
     * @param collection A collection of something
     */
    public static void noDuplicates(String name, Collection<?> collection) {
        if (!(collection instanceof Set)) {
            Set<Object> nue = new HashSet<>(collection);
            if (nue.size() != collection.size()) {
                throw new IllegalArgumentException(name + " contains duplicate entries (" + collection + ")");
            }
        }
    }

    /**
     * Throws an exception if the passed string starts with the passed character
     * @param name The parameter name for constructing the exception message
     * @param value a string or equivalent.  May be null, use notNull() for null checks.
     * @throws IllegalArgumentException if the passed value starts with the passed character
     */
    public static void mayNotStartWith(String name, CharSequence value, char c) {
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
        if (value != null) {
            if (!type.isInstance(value)) {
                throw new ClassCastException(value
                        + " is not an instance of " + type.getName() + " (" + value.getClass().getName() + ")");
            }
        }
    }

    /**
     * Check that the given argument is of the given length, throw an IllegalArgumentException if that is not the case.
     * @param name The parameter name for constructing the exception message
     * @param length The required length of the given value
     * @param value The value to check the length of
     * @throws IllegalArgumentException if the length of the passed in value is not equal to the specified required length
     * @throws NullPointerException if the name is null
     * @throws NullArgumentException if the value is null
     */
    public static void isOfLength(String name, int length, Object[] value) {
        notNull(name, value);
        if (value.length != length) {
            throw new IllegalArgumentException("value " + Strings.toString(value) + " does not have the required arguments of " + length);
        }
    }

    public static void fileExists (File file) {
        notNull("file", file);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException (file + " does not exist or is not a regular file");
        }
    }

    public static void folderExists (File file) {
        notNull("file", file);
        if (!file.exists() || !file.isDirectory()) {
            throw new IllegalArgumentException (file + " does not exist or is not a regular file");
        }
    }

}
