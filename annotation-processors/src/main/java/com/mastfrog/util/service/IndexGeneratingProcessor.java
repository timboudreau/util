/*
 * The MIT License
 *
 * Copyright 2014 tim.
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

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

/**
 * An annotation processor that generates an index correctly, waiting until all
 * rounds of annotation processing have been completed before writing the output
 * so as to avoid a FilerException on opening a file for writing twice in one
 * compile sequence.
 *
 * @author Tim Boudreau
 */
public abstract class IndexGeneratingProcessor<T extends IndexEntry> extends AbstractProcessor {

    private final boolean processOnFinalRound;
    protected final AnnotationIndexFactory<T> indexer;
    protected ProcessingEnvironment processingEnv;

    protected IndexGeneratingProcessor(AnnotationIndexFactory<T> indexer) {
        this(false, indexer);
    }

    protected IndexGeneratingProcessor(boolean processOnFinalRound, AnnotationIndexFactory<T> indexer) {
        this.processOnFinalRound = processOnFinalRound;
        this.indexer = indexer;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        super.init(processingEnv);
    }


    @Override
    public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (roundEnv.errorRaised()) {
                return false;
            }
            if (roundEnv.processingOver()) {
                if (processOnFinalRound) {
                    handleProcess(annotations, roundEnv);
                }
                indexer.write(processingEnv);
                onDone();
                return true;
            } else {
                return handleProcess(annotations, roundEnv);
            }
        } catch (Exception e) {
            if (processingEnv != null) {
                processingEnv.getMessager().printMessage(Kind.ERROR, e.toString());
            }
            e.printStackTrace(System.err);
            return false;
        }
    }

    protected void onDone() {

    }

    protected abstract boolean handleProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv);

    protected final int addIndexElement(String path, T l, Element... el) {
        return indexer.add(path, l, processingEnv, el);
    }
}
