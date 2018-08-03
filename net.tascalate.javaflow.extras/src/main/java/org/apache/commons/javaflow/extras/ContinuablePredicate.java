/**
 * ﻿Copyright 2013-2018 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.extras;

import java.util.Objects;

import org.apache.commons.javaflow.api.continuable;


/**
 * Represents a continuable predicate (boolean-valued function) of one argument.
 *
 * <p>This is a functional interface
 * whose functional method is {@link #test(Object)}.
 *
 * @param <T> the type of the input to the predicate
 *
 */
@FunctionalInterface
public interface ContinuablePredicate<T> {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param t the input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    @continuable boolean test(T t);

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another.  When evaluating the composed
     * predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ANDed with this
     *              predicate
     * @return a composed predicate that represents the short-circuiting logical
     * AND of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    default ContinuablePredicate<T> and(ContinuablePredicate<? super T> other) {
        Objects.requireNonNull(other);
        ContinuablePredicate<T> self = this;
        return new ContinuablePredicate<T>() {
            @Override
            public boolean test(T t) {
                return self.test(t) && other.test(t);
            }
        };
    }

    /**
     * Returns a predicate that represents the logical negation of this
     * predicate.
     *
     * @return a predicate that represents the logical negation of this
     * predicate
     */
    default ContinuablePredicate<T> negate() {
        ContinuablePredicate<T> self = this;
        return new ContinuablePredicate<T>() {
            @Override
            public boolean test(T t) {
                return !self.test(t);
            }
        };
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another.  When evaluating the composed
     * predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ORed with this
     *              predicate
     * @return a composed predicate that represents the short-circuiting logical
     * OR of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    default ContinuablePredicate<T> or(ContinuablePredicate<? super T> other) {
        Objects.requireNonNull(other);
        ContinuablePredicate<T> self = this;
        return new ContinuablePredicate<T>() {
            @Override
            public boolean test(T t) {
                return self.test(t) || other.test(t);
            }
        };
    }

    /**
     * Returns a predicate that tests if two arguments are equal according
     * to {@link Objects#equals(Object, Object)}.
     *
     * @param <T> the type of arguments to the predicate
     * @param targetRef the object reference with which to compare for equality,
     *               which may be {@code null}
     * @return a predicate that tests if two arguments are equal according
     * to {@link Objects#equals(Object, Object)}
     */
    static <T> ContinuablePredicate<T> isEqual(Object targetRef) {
        return new ContinuablePredicate<T>() {
            @Override
            public boolean test(T t) {
                return (null == targetRef) ? t == null : targetRef.equals(t);
            }
        };
    }
}

