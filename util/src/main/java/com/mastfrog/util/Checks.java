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
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * Code sanity checks, to simplify things like null checks. Use these where
 * applicable instead of directly throwing exceptions. That way, expensive tests
 * can be switched to use assertions for production.
 *
 * @author Tim Boudreau
 * @deprecated Split into its own library - us com.mastfrog.util.preconditions.Checks instead
 */
@Deprecated
public final class Checks {

    static boolean disabled = Boolean.getBoolean("checksDisabled");

    private Checks() {
    }

    public static long canCastToInt(String name, long value) {
        return com.mastfrog.util.preconditions.Checks.canCastToInt(name, value);
    }

    public static void atLeastOneNotNull(String msg, Object... objects) {
        com.mastfrog.util.preconditions.Checks.atLeastOneNotNull(msg, objects);
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
        return com.mastfrog.util.preconditions.Checks.notNull(name, val);
    }

    /**
     * Determine that the passed argument is not a negative number
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static <T extends Number> T nonNegative(String name, T val) {
        return com.mastfrog.util.preconditions.Checks.nonNegative(name, val);
    }

    /**
     * Determine that the passed argument is not a negative number
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static int nonNegative(String name, int val) {
        return com.mastfrog.util.preconditions.Checks.nonNegative(name, val);
    }

    /**
     * Determine that the passed argument is not a negative number
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static long nonNegative(String name, long val) {
        return com.mastfrog.util.preconditions.Checks.nonNegative(name, val);
    }

    public static <T extends Number> T greaterThanOne(String name, T val) {
        return com.mastfrog.util.preconditions.Checks.greaterThanOne(name, val);
    }

    /**
     * Determine that the passed argument &gt;=0.
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static int greaterThanZero(String name, int val) {
        return com.mastfrog.util.preconditions.Checks.greaterThanZero(name, val);
    }

    /**
     * Determine that the passed argument &gt;=1.
     *
     * @param name The name of the argument
     * @param val The value
     * @throws IllegalArgumentException if the number is negative
     */
    public static long greaterThanZero(String name, long val) {
        return com.mastfrog.util.preconditions.Checks.greaterThanZero(name, val);
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
        return com.mastfrog.util.preconditions.Checks.notEmptyOrNull(name, array);
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
        return com.mastfrog.util.preconditions.Checks.noNullElements(name, arr);
    }

    /**
     * Throws an exception if a parameter is equal to zero.
     *
     * @param name The name of the parameter
     * @param val A number
     * @throws IllegalArgumentException if the value of the number equals 0
     */
    public static <T extends Number> T nonZero(String name, T val) {
        return com.mastfrog.util.preconditions.Checks.nonZero(name, val);
    }

    /**
     * Throws an exception if a parameter is equal to zero.
     *
     * @param name The name of the parameter
     * @param val A number
     * @throws IllegalArgumentException if the value of the number equals 0
     */
    public static int nonZero(String name, int val) {
        return com.mastfrog.util.preconditions.Checks.nonZero(name, val);
    }

    /**
     * Throws an exception if a parameter is equal to zero.
     *
     * @param name The name of the parameter
     * @param val A number
     * @throws IllegalArgumentException if the value of the number equals 0
     */
    public static long nonZero(String name, long val) {
        return com.mastfrog.util.preconditions.Checks.nonZero(name, val);
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
        return com.mastfrog.util.preconditions.Checks.notEmpty(name, value);
    }

    public static void notEmpty(String name, Collection<?> collection) {
        com.mastfrog.util.preconditions.Checks.notEmpty(name, collection);
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
        return com.mastfrog.util.preconditions.Checks.notNullOrEmpty(name, value);
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
        com.mastfrog.util.preconditions.Checks.mayNotContain(name, value, chars);
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
        com.mastfrog.util.preconditions.Checks.mustContain(name, value, c);
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
        com.mastfrog.util.preconditions.Checks.noDuplicates(name, collection);
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
        com.mastfrog.util.preconditions.Checks.mayNotStartWith(name, value, c);
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
        com.mastfrog.util.preconditions.Checks.isInstance(name, type, value);
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
        com.mastfrog.util.preconditions.Checks.isOfLength(name, length, value);
    }

    /**
     * Test that file exists and is a file not a folder.
     *
     * @param file The file
     */
    public static void fileExists(File file) {
        com.mastfrog.util.preconditions.Checks.fileExists(file);
    }


    /**
     * Test that file exists and is a file not a folder.
     *
     * @param file The file
     * @param paramName The name of the method parameter
     */
    public static File fileExists(String paramName, File file) {
        return com.mastfrog.util.preconditions.Checks.fileExists(paramName, file);
    }

    /**
     * Test that a file exists and is a folder.
     *
     * @param file The file
     */
    public static void folderExists(File file) {
        com.mastfrog.util.preconditions.Checks.folderExists(file);
    }

    /**
     * Test that a file exists and is a folder.
     *
     * @param file The file
     */
    public static File folderExists(String paramName, File file) {
        return com.mastfrog.util.preconditions.Checks.folderExists(paramName, file);
    }


    /**
     * Test that a file exists and the current user has read permission.
     *
     * @param file A file
     */
    public static void readable(File file) {
        com.mastfrog.util.preconditions.Checks.readable(file);
    }

    /**
     * Test that a file exists and the current user has read permission.
     *
     * @param file A file
     */
    public static File readable(String paramName, File file) {
        return com.mastfrog.util.preconditions.Checks.readable(paramName, file);
    }

    /**
     * Test that a file exists and the current user has read permission.
     *
     * @param file A file
     */
    public static File readableAndNonZeroLength(String paramName, File file) {
        return com.mastfrog.util.preconditions.Checks.readableAndNonZeroLength(paramName, file);
    }

    /**
     * Test that a character sequence is encodable in the passe charset.
     *
     * @param seq A string
     * @param charset A character set
     */
    public static void encodable(CharSequence seq, Charset charset) {
        com.mastfrog.util.preconditions.Checks.encodable(seq, charset);
    }
}
