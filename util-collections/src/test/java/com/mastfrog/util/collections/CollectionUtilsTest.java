/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.util.collections;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import static org.junit.Assert.fail;
import org.junit.Test;

public class CollectionUtilsTest {

    @Test
    public void testCollectionsUtilsHasNoInstanceMethods() throws Exception {
        String msg = listInstanceMethods(CollectionUtils.class);
        if (msg != null) {
            fail(msg);
        }
    }

    @Test
    public void testArrayUtilsHasNoInstanceMethods() throws Exception {
        String msg = listInstanceMethods(ArrayUtils.class);
        if (msg != null) {
            fail(msg);
        }
    }

    private static String listInstanceMethods(Class<?> type) throws Exception {
        StringBuilder sb = new StringBuilder();
        Method[] methods = type.getDeclaredMethods();
        for (Method m : methods) {
            m.setAccessible(true);
            if ((m.getModifiers() & Modifier.STATIC) == 0) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(m.getName());
                if (m.getParameterCount() > 0) {
                    Class<?>[] types = m.getParameterTypes();
                    sb.append("(");
                    for (int i = 0; i < types.length; i++) {
                        sb.append(types[i].getSimpleName());
                        if (i != types.length-1) {
                            sb.append(", ");
                        }
                    }
                    sb.append(')');
                } else {
                    sb.append("()");
                }
            }
        }
        if (sb.length() > 0) {
            sb.insert(0, "Found instance methods on " + type.getSimpleName() + ": ");
        }
        return sb.length() == 0 ? null : sb.toString();
    }

}
