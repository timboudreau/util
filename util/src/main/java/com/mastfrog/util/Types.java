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

import com.mastfrog.util.thread.Receiver;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
public class Types {

    private Types() {
    }

    public static void visit(Class<?> type, Receiver<Class<?>> r) {
        Set<Class<?>> visited = new HashSet<>();
        while (type != null && type != Object.class) {
            r.receive(type);
            for (Class<?> c : type.getInterfaces()) {
                if (!visited.contains(c)) {
                    visited.add(c);
                    r.receive(c);
                }
                visited.add(c);
            }
            visited.add(type);
            type = type.getSuperclass();
        }
    }
    
    public static Set<Class<?>> get(Class<?> type) {
        final Set<Class<?>> result = new HashSet<>();
        visit(type, new Receiver<Class<?>> (){

            @Override
            public void receive(Class<?> object) {
                result.add(object);
            }
        });
        if (Object.class.isAssignableFrom(type)) {
            result.add(Object.class);
        }
        return result;
    }
    
    public static String list(Class<?> type) {
        Set<Class<?>> c = get(type);
        StringBuilder sb = new StringBuilder();
        for (Iterator<Class<?>> it = c.iterator(); it.hasNext();) {
        Class<?> cc = it.next();
            sb.append(cc.getSimpleName());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
