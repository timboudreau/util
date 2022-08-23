/*
 * Copyright 2016-2019 Tim Boudreau, Frédéric Yvon Vinet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastfrog.function.character.anno;

import com.mastfrog.function.character.CharPredicate;
import com.mastfrog.function.character.CharPredicates;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.CLASS;
import java.lang.annotation.Target;

/**
 * Annotation to flexibly define a character predicates. If multiple are
 * defined, they will be or'd together unless logicallyOr is false, in which
 * case they will be logically and'd.
 * <p>
 * This is used in the ANTLR NetBeans plugin to allow declarative definition of
 * conditions in which various editor features can be applied.
 * </p>
 *
 * @author Tim Boudreau
 */
@Retention(CLASS)
@Target({})
public @interface CharPredicateSpec {

    /**
     * Include some characters which will cause the resulting predicate to
     * return true.
     *
     * @return An array of characters
     */
    char[] including() default {};

    /**
     * A character predicate to include.
     *
     * @return A type
     */
    Class<? extends CharPredicate> instantiate() default CharPredicate.class;

    /**
     * An array of built-in CharPredicates to include.
     *
     * @return An array of CharPredicates
     */
    CharPredicates[] include() default {};

    /**
     * Combine all the predicates defined in this annotation using logical or if
     * true.
     *
     * @return true if they should be or'd
     */
    boolean logicallyOr() default true;

    /**
     * If true, negate the resulting predicate.
     *
     * @return
     */
    boolean negated() default false;
}
