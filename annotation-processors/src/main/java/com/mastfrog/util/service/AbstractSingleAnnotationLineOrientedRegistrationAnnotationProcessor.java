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

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;

/**
 *
 * @author Tim Boudreau
 */
public abstract class AbstractSingleAnnotationLineOrientedRegistrationAnnotationProcessor<T extends Annotation> extends AbstractLineOrientedRegistrationAnnotationProcessor {

    private final Class<T> annotationType;

    public AbstractSingleAnnotationLineOrientedRegistrationAnnotationProcessor(Class<T> annotationType, String... annotationLegalOnFqns) {
        super(annotationLegalOnFqns);
        this.annotationType = annotationType;
    }

    protected Set<Element> findAnnotatedElements(RoundEnvironment roundEnv) {
        return new HashSet<>( roundEnv.getElementsAnnotatedWith( annotationType )) ;
    }

    @Override
    protected final int getOrder(Annotation anno) {
        return order(annotationType.cast(anno));
    }

    protected int order(T anno) {
        return 0;
    }

    @Override
    protected boolean isAcceptable(Annotation anno) {
        return annotationType.isInstance(anno);
    }

    @Override
    protected T findAnnotation(Element e) {
        return e.getAnnotation(annotationType);
    }

    @Override
    protected final void handleOne(Element e, Annotation anno, int order) {
        doHandleOne(e, annotationType.cast(anno), order);
    }

    protected abstract void doHandleOne(Element e, T anno, int order);

}
