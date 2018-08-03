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

import java.util.Comparator;
import java.util.Objects;


/**
 * Represents an continuable operation upon two operands of the same type, producing a result
 * of the same type as the operands.  This is a specialization of
 * {@link ContinuableBiFunction} for the case where the operands and the result are all of
 * the same type.
 *
 * <p>This is a functional interface
 * whose functional method is {@link #apply(Object, Object)}.
 *
 * @param <T> the type of the operands and result of the operator
 *
 * @see ContinuableBiFunction
 * @see ContinuableUnaryOperator
 */
@FunctionalInterface
public interface ContinuableBinaryOperator<T> extends ContinuableBiFunction<T,T,T> {
    /**
     * Returns a {@link ContinuableBinaryOperator} which returns the lesser of two elements
     * according to the specified {@code Comparator}.
     *
     * @param <T> the type of the input arguments of the comparator
     * @param comparator a {@code Comparator} for comparing the two values
     * @return a {@code ContinuableBinaryOperator} which returns the lesser of its operands,
     *         according to the supplied {@code Comparator}
     * @throws NullPointerException if the argument is null
     */
    public static <T> ContinuableBinaryOperator<T> minBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return new ContinuableBinaryOperator<T>() {
            @Override
            public T apply(T a, T b) {
                return comparator.compare(a, b) <= 0 ? a : b;
            }
            
        };

    }

    /**
     * Returns a {@link ContinuableBinaryOperator} which returns the greater of two elements
     * according to the specified {@code Comparator}.
     *
     * @param <T> the type of the input arguments of the comparator
     * @param comparator a {@code Comparator} for comparing the two values
     * @return a {@code ContinuableBinaryOperator} which returns the greater of its operands,
     *         according to the supplied {@code Comparator}
     * @throws NullPointerException if the argument is null
     */
    public static <T> ContinuableBinaryOperator<T> maxBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return new ContinuableBinaryOperator<T>() {
            @Override
            public T apply(T a, T b) {
                return comparator.compare(a, b) >= 0 ? a : b;
            }
            
        };
    }
}