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

import com.mastfrog.annotation.AnnotationUtils;
import com.mastfrog.annotation.registries.AbstractLineOrientedRegistrationAnnotationProcessor;
import com.mastfrog.annotation.registries.Line;
import static com.mastfrog.util.service.ServiceProviderAnnotationProcessor.SERVICE_PROVIDER_ANNOTATION_FQN;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 *
 * @author Tim Boudreau
 */
@SupportedAnnotationTypes(SERVICE_PROVIDER_ANNOTATION_FQN)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class ServiceProviderAnnotationProcessor extends AbstractLineOrientedRegistrationAnnotationProcessor {

    public static final String SERVICE_PROVIDER_ANNOTATION_FQN
            = "com.mastfrog.util.service.ServiceProvider";

    private static final int DEFAULT_ORDER = Integer.MAX_VALUE / 2;

    public ServiceProviderAnnotationProcessor() {
        super(true);
    }

    @Override
    protected int getOrder(AnnotationMirror anno) {
        Integer val = utils().annotationValue(anno, "order", Integer.class);
        return val == null ? DEFAULT_ORDER : val;
    }

    @Override
    protected void handleOne(Element e, AnnotationMirror anno, int order, AnnotationUtils utils) {
        Set<Modifier> modifiers = e.getModifiers();
        if (!(e instanceof TypeElement)) {
            fail("@ServiceProvider is only applicable to classes");
            return;
        }
        if (modifiers.contains(Modifier.ABSTRACT)) {
            fail("Service provider types may not be abstract", e);
            return;
        }
        if (!modifiers.contains(Modifier.PUBLIC)) {
            fail("Service provider types must be public");
            return;
        }
        TypeElement type = (TypeElement) e;
        if (checkPublicConstructor(type)) {
            List<String> classNames = utils.classNamesForAnnotationMember(e, SERVICE_PROVIDER_ANNOTATION_FQN, "value", legalOn);
            if (classNames.isEmpty()) {
                fail("No class name found for service registration", e, anno);
                return;
            }
            boolean specifiesOrder = order != DEFAULT_ORDER;
            String msg = "Generating META-INF/services registration(s) for " + type.getQualifiedName()
                    + " as a " + AnnotationUtils.join(',', classNames);
            if (specifiesOrder) {
                msg += " with order " + order;
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg, e);
            classNames.forEach((name) -> {
                String typeName = utils.canonicalize(e.asType());
                if (typeName == null) {
                    fail("Could not canonicalize " + e, e);
                    return;
                }
                String lineWithPositionCommentAppended = specifiesOrder ? typeName + "\n#position=" + order : typeName;
                Line line = new Line(order, new Element[]{e}, lineWithPositionCommentAppended);
                indexer.add("META-INF/services/" + name, line, processingEnv, e);
            });
        } else {
            fail("Classes annotated with @ServiceProvider must have a default constructor, either implicitly (no declared constructors), or "
                    + "explicitly (one constructor which is public and takes no arguments)", e, anno);
        }
    }

    private boolean checkPublicConstructor(TypeElement e) {
        boolean foundDefaultConstructor = false;
        outer:
        for (Element member : e.getEnclosedElements()) {
            switch (member.getKind()) {
                case CONSTRUCTOR:
                    ExecutableElement exe = (ExecutableElement) member;
                    List<? extends VariableElement> params = exe.getParameters();
                    if (params == null || params.isEmpty()) {
                        foundDefaultConstructor = true;
                        if (!exe.getModifiers().contains(Modifier.PUBLIC)) {
                            fail("Default constructor must be public on classes annotated with @ServiceProvider", exe);
                            return false;
                        }
                        break outer;
                    }
                    break;
                default:

            }
        }
        return foundDefaultConstructor;
    }
}
