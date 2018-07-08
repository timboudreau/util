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
import java.lang.annotation.AnnotationTypeMismatchException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Useful methods for extracting information from Java sources in an annotation
 * processor.
 *
 * @author Tim Boudreau
 */
public final class AnnotationUtils {

    private final ProcessingEnvironment processingEnv;
    private final Set<String> supportedAnnotationTypes;

    public AnnotationUtils(ProcessingEnvironment processingEnv, Set<String> supportedAnnotationTypes) {
        this.processingEnv = processingEnv;
        this.supportedAnnotationTypes = supportedAnnotationTypes == null
                ? Collections.emptySet() : new HashSet<>(supportedAnnotationTypes);
    }

    public enum SubtypeResult {
        TRUE,
        FALSE,
        TYPE_NAME_NOT_RESOLVABLE;

        public boolean isSubtype() {
            return this == TRUE;
        }
    }

    /**
     * Determine if the passed element is a subtype of the passed type name.
     * Will return false if the class is not a subtype, <i>or if the passed type
     * name is not resolvable on the compiler's classpath</i>.
     *
     * @param e A source element
     * @param typeName A fully qualified type name
     * @return
     */
    public SubtypeResult isSubtypeOf(Element e, String typeName) {
        Types types = processingEnv.getTypeUtils();
        Elements elementUtils = processingEnv.getElementUtils();
        TypeElement pageType = elementUtils.getTypeElement(typeName);
        if (pageType == null) {
            return SubtypeResult.TYPE_NAME_NOT_RESOLVABLE;
        }
        return types.isSubtype(e.asType(), pageType.asType()) ? SubtypeResult.TRUE : SubtypeResult.FALSE;
    }

    /**
     * Find the AnnotationMirror for a specific annotation type.
     *
     * @param el The annotated element
     * @param annotationTypeFqn The fully qualified name of the annotation class
     * @return An annotation mirror
     */
    public AnnotationMirror findMirror(Element el, String annotationTypeFqn) {
        for (AnnotationMirror mir : el.getAnnotationMirrors()) {
            TypeMirror type = mir.getAnnotationType().asElement().asType();
            String typeName = canonicalize(type);
            if (annotationTypeFqn.equals(typeName)) {
                return mir;
            }
        }
        return null;
    }

    /**
     * Find the AnnotationMirror for a specific annotation type.
     *
     * @param el The annotated element
     * @param annoType The annotation type
     * @return An annotation mirror
     */
    public AnnotationMirror findMirror(Element el,
            Class<? extends Annotation> annoType) {
        for (AnnotationMirror mir : el.getAnnotationMirrors()) {
            TypeMirror type = mir.getAnnotationType().asElement().asType();
            if (annoType.getName().equals(type.toString())) {
                return mir;
            }
        }
        return null;
    }

    /**
     * Failover stringification.
     *
     * @param m A type mirror
     * @return A string representation
     */
    private String stripGenericsFromStringRepresentation(TypeMirror m) {
        String result = m.toString();
        result = result.replaceAll("\\<.*\\>", "");
        return result;
    }

    /**
     * Convert a class name to a loadable one which delimits nested class names
     * using $ - i.e. transforms com.foo.MyClass.MyInner.MyInnerInner to
     * com.foo.MyClass$MyInner$MyInnerInner.
     *
     * @param tm The type
     * @return A string representation
     */
    public String canonicalize(TypeMirror tm) {
        Types types = processingEnv.getTypeUtils();
        Element maybeType = types.asElement(tm);
        if (maybeType == null) {
            if (tm.getKind().isPrimitive()) {
                return types.getPrimitiveType(tm.getKind()).toString();
            } else {
                return stripGenericsFromStringRepresentation(tm);
            }
        }
        if (maybeType instanceof TypeParameterElement) {
            maybeType = ((TypeParameterElement) maybeType).getGenericElement();
        }
        if (maybeType instanceof TypeElement) {
            TypeElement e = (TypeElement) maybeType;
            StringBuilder nm = new StringBuilder(e.getQualifiedName().toString());
            Element enc = e.getEnclosingElement();
            while (enc != null && enc.getKind() != ElementKind.PACKAGE) {
                int ix = nm.lastIndexOf(".");
                if (ix > 0) {
                    nm.setCharAt(ix, '$');
                }
                enc = enc.getEnclosingElement();
            }
            return nm.toString();
        }

        warn("Cannot canonicalize " + tm);
        return null;
    }

    /**
     * Print a warning.
     *
     * @param warning The warning
     */
    public void warn(String warning) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, warning);
    }

    public static String types(Object o) { //debug stuff
        List<String> s = new ArrayList<>();
        Class<?> x = o.getClass();
        while (x != Object.class) {
            s.add(x.getName());
            for (Class<?> c : x.getInterfaces()) {
                s.add(c.getName());
            }
            x = x.getSuperclass();
        }
        StringBuilder sb = new StringBuilder();
        for (String ss : s) {
            sb.append(ss);
            sb.append(", ");
        }
        return sb.toString();
    }

    public static String join(char delim, String... strings) {
        StringBuilder sb = new StringBuilder(strings.length * 20);
        for (int i = 0; i < strings.length; i++) {
            sb.append(strings[i]);
            if (i != strings.length - 1) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }

    public static String join(char delim, Iterable<String> strings) {
        StringBuilder sb = new StringBuilder(250);
        for (Iterator<String> it = strings.iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    /**
     * Get a list of fully qualified, canonicalized Java class names for a
     * member of an annotation. Typically such classes cannnot actually be
     * loaded by the compiler, so referencing the Class objects for them results
     * in a compile failure; this method produces string representations of such
     * names without relying on being able to load the classes in question.
     * Handles the case that the annotation member is not an array by returning
     * a one-element list.
     *
     * @param mirror An annotation mirror
     * @param param The annotation's method which should be queried
     * @param failIfNotSubclassesOf Fail the compile if the types listed are not
     * subclasses of one of the passed type FQNs.
     * @return A list of stringified, canonicalized type names
     */
    public List<String> typeList(AnnotationMirror mirror, String param, String... failIfNotSubclassesOf) {
        List<String> result = new ArrayList<>();
        if (mirror != null) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> x : mirror.getElementValues()
                    .entrySet()) {
                String annoParam = x.getKey().getSimpleName().toString();
                if (param.equals(annoParam)) {
                    if (x.getValue().getValue() instanceof List<?>) {
                        List<?> list = (List<?>) x.getValue().getValue();
                        for (Object o : list) {
                            if (o instanceof AnnotationValue) {
                                AnnotationValue av = (AnnotationValue) o;
                                if (av.getValue() instanceof DeclaredType) {
                                    DeclaredType dt = (DeclaredType) av.getValue();
                                    if (failIfNotSubclassesOf.length > 0) {
                                        boolean found = false;
                                        for (String f : failIfNotSubclassesOf) {
                                            if (!isSubtypeOf(dt.asElement(), f).isSubtype()) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            fail("Not a " + join('/', failIfNotSubclassesOf) + " subtype: "
                                                    + av);
                                            continue;
                                        }
                                    }
                                    // Convert e.g. mypackage.Foo.Bar.Baz to mypackage.Foo$Bar$Baz
                                    String canonical = canonicalize(dt.asElement().asType());
                                    if (canonical == null) {
                                        // Unresolvable generic or broken source
                                        fail("Could not canonicalize " + dt.asElement(), x.getKey());
                                    } else {
                                        result.add(canonical);
                                    }
                                } else {
                                    // Unresolvable type?
                                    warn("Not a declared type: " + av);
                                }
                            } else {
                                // Probable invalid source
                                warn("Annotation value for value() is not an AnnotationValue " + types(o));
                            }
                        }
                    } else if (x.getValue().getValue() instanceof DeclaredType) {
                        DeclaredType dt = (DeclaredType) x.getValue().getValue();
                        if (failIfNotSubclassesOf.length > 0) {
                            boolean found = false;
                            for (String f : failIfNotSubclassesOf) {
                                if (isSubtypeOf(dt.asElement(), f).isSubtype()) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                fail("Not a " + join('/', failIfNotSubclassesOf) + " subtype: " + dt);
                                continue;
                            }
                        }
                        // Convert e.g. mypackage.Foo.Bar.Baz to mypackage.Foo$Bar$Baz
                        String canonical = canonicalize(dt.asElement().asType());
                        if (canonical == null) {
                            // Unresolvable generic or broken source
                            fail("Could not canonicalize " + dt, x.getKey());
                        } else {
                            result.add(canonical);
                        }
                    } else {
                        warn("Annotation value for is not a List of types or a DeclaredType on " + mirror + " - "
                                + types(x.getValue().getValue()) + " - invalid source?");
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get a list of values for a particular annotation member, cast as the
     * passed type.
     *
     * @param <T> The member type
     * @param mirror The type mirror
     * @param param The annotation method name
     * @param type The type to cast the result as (build will fail if it is of
     * the wrong type, which is possible on broken sources)
     * @return A list of instanceof of T
     */
    public <T> List<T> annotationValues(AnnotationMirror mirror, String param, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (mirror != null) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> x : mirror.getElementValues()
                    .entrySet()) {
                String annoParam = x.getKey().getSimpleName().toString();
                if (param.equals(annoParam)) {
                    if (x.getValue().getValue() instanceof List<?>) {
                        List<?> list = (List<?>) x.getValue().getValue();
                        for (Object o : list) {
                            if (o instanceof AnnotationValue) {
                                AnnotationValue av = (AnnotationValue) o;
                                try {
                                    result.add(type.cast(av.getValue()));
                                } catch (ClassCastException | AnnotationTypeMismatchException ex) {
                                    fail("Not an instance of " + type.getName() + " for value of "
                                            + param + " on " + mirror.getAnnotationType()
                                            + " found a " + x.getValue().getClass().getName()
                                            + " instead: " + ex.getMessage(), x.getKey());
                                }
                            } else {
                                // Probable invalid source
                                warn("Annotation value for value() is not an AnnotationValue " + types(o));
                            }
                        }
                    } else {
                        try {
                            result.add(type.cast(x.getValue().getValue()));
                        } catch (ClassCastException | AnnotationTypeMismatchException ex) {
                            fail("Not an instance of " + type.getName() + " for value of " + param + " on "
                                    + mirror.getAnnotationType() + " found a " + x.getValue().getClass().getName()
                                    + " instead: " + ex.getMessage(), x.getKey());
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get the value of a method on an annotation, where the return type is
     * <i>not an array</i>.
     *
     * @param <T> The type to cast as
     * @param mirror The annotation mirror
     * @param param The name of the annotation's method whose value should be
     * returned
     * @param type The type to cast each value to
     * @param defaultValue Value to return if no such parameter is found
     * @return A single instance of the value type or null
     */
    public <T> T annotationValue(AnnotationMirror mirror, String param, Class<T> type, T defaultValue) {
        T result = annotationValue(mirror, param, type);
        return result == null ? defaultValue : result;
    }

    /**
     * Get the value of a method on an annotation, where the return type is
     * <i>not an array</i>.
     *
     * @param <T> The type to cast as
     * @param mirror The annotation mirror
     * @param param The name of the annotation's method whose value should be
     * returned
     * @param type The type to cast each value to
     * @return A single instance of the value type or null
     */
    public <T> T annotationValue(AnnotationMirror mirror, String param, Class<T> type) {
        T result = null;
        if (mirror != null) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> x : mirror.getElementValues()
                    .entrySet()) {
                String annoParam = x.getKey().getSimpleName().toString();
                if (param.equals(annoParam)) {
                    if (x.getValue().getValue() instanceof List<?>) {
                        List<?> list = (List<?>) x.getValue().getValue();
                        for (Object o : list) {
                            if (o instanceof AnnotationValue) {
                                AnnotationValue av = (AnnotationValue) o;
                                try {
                                    result = type.cast(av.getValue());
                                    break;
                                } catch (ClassCastException | AnnotationTypeMismatchException ex) {
                                    fail("Not an instance of " + type.getName() + " for value of " + param + " on "
                                            + mirror.getAnnotationType()
                                            + ", but was " + type.getName() + ": " + ex.getMessage(), x.getKey());
                                }
                            } else {
                                result = type.cast(result);
                            }
                        }
                    } else {
                        try {
                            result = type.cast(x.getValue().getValue());
                        } catch (ClassCastException | AnnotationTypeMismatchException ex) {
                            fail("Not an instance of " + type.getName() + " for value of "
                                    + param + " on " + mirror.getAnnotationType()
                                    + ", but was " + type.getName()
                                    + ": " + ex.getMessage(), x.getKey());
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Look up an annotation by type, call a method on that annotation which can
     * return one or an array of Class objects, and return the class names as a
     * list of strings.
     *
     * @param el The element that is annotated
     * @param annotationType The annotation type
     * @param memberName The method on the annotation whose value is sought
     * @param mustBeSubtypesOf If this array is non-empty, fail the build if any
     * value found is not a subtype of one of the passed fully-qualified Java
     * class names
     * @return A list of canonicalized Java class names
     */
    public List<String> classNamesForAnnotationMember(Element el,
            Class<? extends Annotation> annotationType, String memberName,
            String... mustBeSubtypesOf) {
        AnnotationMirror mirror = findMirror(el, annotationType);
        List<String> result;
        if (mirror != null) {
            result = typeList(mirror, memberName, mustBeSubtypesOf);
        } else {
            result = Collections.emptyList();
        }
        return result;
    }

    /**
     * Look up an annotation by fully qualified type name, call a method on that
     * annotation which can return one or an array of Class objects, and return
     * the class names as a list of strings.
     *
     * @param el The element that is annotated
     * @param annotationType The annotation type
     * @param memberName The method on the annotation whose value is sought
     * @param mustBeSubtypesOf If this array is non-empty, fail the build if any
     * value found is not a subtype of one of the passed fully-qualified Java
     * class names
     * @return A list of canonicalized Java class names
     */
    public List<String> classNamesForAnnotationMember(Element el,
            String annotationTypeFqn, String memberName,
            String... mustBeSubtypesOf) {
        AnnotationMirror mirror = findMirror(el, annotationTypeFqn);
        List<String> result = typeList(mirror, memberName, mustBeSubtypesOf);
        return result == null ? Collections.emptyList() : result;
    }

    /**
     * Look up an annotation by fully qualified type name, call a method on that
     * annotation which can return one or an array of Class objects, and return
     * the class names as a list of strings.
     *
     * @param el The element that is annotated
     * @param annotationType The annotation type
     * @param memberName The method on the annotation whose value is sought
     * @param mustBeSubtypesOf If this array is non-empty, fail the build if any
     * value found is not a subtype of one of the passed fully-qualified Java
     * class names
     * @return A list of canonicalized Java class names
     */
    public List<String> classNamesForAnnotationMember(Element el,
            String annotationTypeFqn, String memberName,
            Collection<String> mustBeSubtypesOf) {
        String[] subtypesOf = mustBeSubtypesOf.toArray(new String[mustBeSubtypesOf.size()]);
        AnnotationMirror mirror = findMirror(el, annotationTypeFqn);
        List<String> result;
        if (mirror != null) {
            result = typeList(mirror, memberName, subtypesOf);
        } else {
            result = Collections.emptyList();
        }
        return result;
    }

    /**
     * Fail the build with a message tied to a particular element.
     *
     * @param msg The message
     * @param el The element
     */
    public void warn(String msg, Element el, AnnotationMirror mir) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg, el, mir);
    }

    /**
     * Fail the build with a message tied to a particular element.
     *
     * @param msg The message
     * @param el The element
     */
    public void warn(String msg, Element el) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg, el);
    }

    /**
     * Fail the build with a message tied to a particular element.
     *
     * @param msg The message
     * @param el The element
     */
    public void fail(String msg, Element el) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, el);
    }

    /**
     * Fail the build with a message tied to a particular element.
     *
     * @param msg The message
     * @param el The element
     */
    public void fail(String msg, Element el, AnnotationMirror mir) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, el, mir);
    }

    /**
     * Fail the build with a message
     *
     * @param msg The message
     */
    public void fail(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

    /**
     * Find elements annotated with the set of supported annotation types passed
     * to the constructor.
     *
     * @param roundEnv The environment
     * @return A set of elements
     */
    public Set<Element> findAnnotatedElements(RoundEnvironment roundEnv) {
        return findAnnotatedElements(roundEnv, supportedAnnotationTypes);
    }

    /**
     * Find elements annotated with the set of supported annotation types passed
     * to the constructor.
     *
     * @param roundEnv The environment for this round of processing
     * @param typeOne A type to look for as a fully qualified java class name
     * @param more More types to look for as a fully qualified java class name
     * @return A set of all elements annotated with one or more of the passed
     * types
     */
    public Set<Element> findAnnotatedElements(RoundEnvironment roundEnv, String typeOne, String... more) {
        Set<String> fqns = new LinkedHashSet<>(more.length + 1);
        fqns.add(typeOne);
        fqns.addAll(Arrays.asList(more));
        return findAnnotatedElements(roundEnv, fqns);
    }

    /**
     * Find elements annotated with the set of supported annotation types passed
     * to the constructor.
     *
     * @param roundEnv The environment
     * @return A set of elements
     */
    public Set<Element> findAnnotatedElements(RoundEnvironment roundEnv, Iterable<String> annotationTypes) {
        Set<Element> result = new HashSet<>(20);
        Elements elementUtils = processingEnv.getElementUtils();

        warnLog("$$$$$$ FIND ANNOTATED ELEMENTS");
        for (String typeName : annotationTypes) {
            warnLog("$$$$$$   CHECK " + typeName);
            TypeElement currType = elementUtils.getTypeElement(typeName);
            if (currType != null) {
                Set<? extends Element> allWith = roundEnv.getElementsAnnotatedWith(currType);
                warnLog("$$$$$$   FOUND " + allWith.size() + " elements annotated with " + typeName);;
                result.addAll(allWith);
            } else {
                warnLog("$$$$$$    NOT ON CLASSPATH: " + currType);
            }
        }
        warnLog("$$$ TOTAL ANNOTATED ELEMENTS: " + result.size());
        return result;
    }

    /**
     * Find all annotation mirrors
     *
     * @param e
     * @return
     */
    public Set<AnnotationMirror> findAnnotationMirrors(Element e) {
        return findAnnotationMirrors(e, supportedAnnotationTypes);
    }

    public AnnotationMirror findAnnotationMirror(Element e, String annotationFqn) {
        Set<AnnotationMirror> mirrors = findAnnotationMirrors(e, annotationFqn);
        if (mirrors.size() > 1) {
            fail("Found more than one annotation of type " + annotationFqn + " on " + e, e, mirrors.iterator().next());
        }
        AnnotationMirror result = mirrors.isEmpty() ? null : mirrors.iterator().next();
        return result;
    }

    public Set<AnnotationMirror> findAnnotationMirrors(Element e, String firstAnnotationTypeFqn, String... moreFqns) {
        Set<String> fqns = new LinkedHashSet<>();
        fqns.add(firstAnnotationTypeFqn);
        fqns.addAll(Arrays.asList(moreFqns));
        return findAnnotationMirrors(e, fqns);
    }

    public Set<AnnotationMirror> findAnnotationMirrors(Element e, Iterable<String> annotationTypeFqns) {
        Set<AnnotationMirror> annos = new LinkedHashSet<>();
        Elements elementUtils = processingEnv.getElementUtils();
        warnLog("$$$$$$ FIND ANNOTATION MIRRORS ON " + e.getSimpleName());
        for (String typeName : annotationTypeFqns) {
            TypeElement currType = elementUtils.getTypeElement(typeName);
            warnLog("$$$$$$   CHECK FOR " + typeName + " found? " + (currType != null));
            if (currType != null) {
                for (AnnotationMirror mirror : e.getAnnotationMirrors()) {
                    warnLog("$$$$$$     FOUND MIRROR " + mirror.getAnnotationType().asElement().getSimpleName());

                    TypeElement test = (TypeElement) mirror.getAnnotationType().asElement();
                    warnLog("$$$$$$      MIRROR ANNOTATION TYPE IS " + (test == null ? "<none>" : test.getQualifiedName()));
                    if (test != null) {
                        if (test.getQualifiedName().equals(currType.getQualifiedName())) {
                            annos.add(mirror);
                        }
                    }
                }
            }
        }
        return annos;
    }

    private static boolean warnLog = false;

    void warnLog(String val) {
        if (warnLog) {
            warn(val);
        }
    }

}
