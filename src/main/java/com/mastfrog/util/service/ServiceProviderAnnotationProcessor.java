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

import java.util.List;
import java.util.Set;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

/**
 *
 * @author Tim Boudreau
 */
@SupportedAnnotationTypes("com.mastfrog.util.service.ServiceProvider")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public final class ServiceProviderAnnotationProcessor extends AbstractRegistrationAnnotationProcessor<ServiceProvider> {

    public ServiceProviderAnnotationProcessor() {
        super(ServiceProvider.class, "java.lang.Object");
    }

    @Override
    protected int getOrder(ServiceProvider anno) {
        return anno.order();
    }

    @Override
    protected void handleOne(Element e, ServiceProvider anno, int order) {
        Set<Modifier> modifiers = e.getModifiers();
        if (modifiers.contains(Modifier.ABSTRACT)) {
            fail("Service provider types may not be abstract", e);
            return;
        }
        if (!modifiers.contains(Modifier.PUBLIC)) {
            fail("Service provider types must be public");
            return;
        }
        List<String> classNames = super.classNames(e, ServiceProvider.class, "value");
        System.out.println("HANDLE ONE: " + classNames);
        classNames.forEach((name) -> {
            addLine("META-INF/services/" + name, e.asType().toString(), e);
        });
    }
}
