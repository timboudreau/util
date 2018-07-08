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

package com.mastfrog.util.service;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 *
 * @author Tim Boudreau
 */
public final class Line implements IndexEntry {

    final int index;
    private Element[] el;
    private final String line;

    public Line(int index, Element[] el, String line) {
        this.index = index;
        this.el = el;
        this.line = line;
    }

    protected static String comment(String line) {
        return "# " + line;
    }

    @Override
    public int compareTo(IndexEntry other) {
        Integer mine = index;
        return mine.compareTo(((Line) other).index);
    }

    void write(PrintWriter w) {
        String origin = origin();
        if (origin != null && !line.contains(origin)) {
            w.println(comment(origin));
        }
        w.println(line);
    }

    @Override
    public Element[] elements() {
        return el;
    }

    String origin() {
        if (el.length == 1) {
            if (el[0] instanceof TypeElement) {
                return ((TypeElement) el[0]).getQualifiedName().toString();
            } else if (el[0] instanceof PackageElement) {
                return ((PackageElement) el[0]).getQualifiedName().toString();
            }
        } else {
            StringBuilder sb = new StringBuilder();
            for (Element e : el) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                if (e instanceof PackageElement) {
                    PackageElement pe = (PackageElement) e;
                    sb.append(pe.getQualifiedName().toString());
                } else if (e instanceof TypeElement) {
                    TypeElement te = (TypeElement) e;
                    sb.append(te.getQualifiedName().toString());
                }
            }
            return sb.length() == 0 ? null : sb.toString();
        }
        return null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        String origin = origin();
        if (origin != null && !line.contains(origin)) {
            sb.append(comment(origin)).append('\n');
        }
        sb.append(line).append("\n");
        return sb.toString();
    }

    public boolean equals(Object o) {
        return o instanceof Line && ((Line) o).line.equals(line);
    }

    public int hashCode() {
        return line.hashCode();
    }

    @Override
    public void addElements(Element... els) {
        Set<Element> initial = new HashSet<>(Arrays.asList(this.el));
        initial.addAll(Arrays.asList(els));
        this.el = initial.toArray(new Element[initial.size()]);
    }
}
