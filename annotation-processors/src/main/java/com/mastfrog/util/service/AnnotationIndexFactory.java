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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 *
 * @author Tim Boudreau
 */
public abstract class AnnotationIndexFactory<T extends IndexEntry> {

    protected final Map<Filer, Map<String, AnnotationIndex<T>>> indexesByProcessor = new HashMap<>();

    public static LineIndexFactory lines() {
        return new LineIndexFactory();
    }

    public int totalSize() {
        int result = 0;
        for (Map.Entry<Filer, Map<String, AnnotationIndex<T>>> indexes : indexesByProcessor.entrySet()) {
            for (Map.Entry<String, AnnotationIndex<T>> ixForPath : indexes.getValue().entrySet()) {
                result += ixForPath.getValue().size();
            }
        }
        return result;
    }

    public final AnnotationIndex<T> get(String path, ProcessingEnvironment processingEnv) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        Filer filer = processingEnv.getFiler();
        Map<String, AnnotationIndex<T>> forFiler = indexesByProcessor.get(filer);
        if (forFiler == null) {
            forFiler = new HashMap<>();
            indexesByProcessor.put(filer, forFiler);
        }
        AnnotationIndex<T> result = forFiler.get(path);
        if (result == null) {
            result = newIndex(path);
            forFiler.put(path, result);
        }
        return result;
    }

    public void write(ProcessingEnvironment processingEnv) {
        try {
            for (Map.Entry<Filer, Map<String, AnnotationIndex<T>>> outputFiles : indexesByProcessor.entrySet()) {
                Filer filer = outputFiles.getKey();
                for (Map.Entry<String, AnnotationIndex<T>> entry : outputFiles.getValue().entrySet()) {
                    try {
                        List<Element> elements = new ArrayList<>();
                        for (T line : entry.getValue()) {
                            elements.addAll(Arrays.asList(line.elements()));
                        }
                        FileObject out = filer.createResource(StandardLocation.CLASS_OUTPUT, "", entry.getKey(),
                                elements.toArray(new Element[0]));
                        try (OutputStream os = out.openOutputStream()) {
                            entry.getValue().write(os, processingEnv);
                        }
                    } catch (IOException x) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write to " + entry.getKey() + ": " + x.toString());
                    }
                }
            }
        } finally {
            indexesByProcessor.clear();
        }
    }

    public final int add(String path, T entry, ProcessingEnvironment processingEnv, Element... associatedWith) {
        return get(path, processingEnv).add(entry);
    }

    protected abstract AnnotationIndex<T> newIndex(String path);

    public static abstract class AnnotationIndex<T extends IndexEntry> implements Iterable<T> {

        private SortedSet<T> entries = new TreeSet<>();

        public final int size() {
            return entries.size();
        }

        public int add(T entry) {
            List<Element> els = new ArrayList<>(5);
            for (T existing : entries) {
                if (entry.equals(existing)) {
                    els.addAll(Arrays.asList(existing.elements()));
                }
            }
            if (!els.isEmpty()) {
                entry.addElements(els.toArray(new Element[0]));
            }
            // Will replace the existing entry
            entries.add(entry);
            return entries.size();
        }

        public abstract void write(OutputStream out, ProcessingEnvironment processingEnv) throws IOException;

        @Override
        public final Iterator<T> iterator() {
            return Collections.unmodifiableSet(entries).iterator();
        }
    }

}
