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
 * Represents a continuable operation that accepts two input arguments and returns no
 * result.  This is the two-arity specialization of {@link ContinuableConsumer}.
 * Unlike most other functional interfaces, {@code ContinuableBiConsumer} is expected
 * to operate via side-effects.
 *
 * <p>This is a functional interface
 * whose functional method is {@link #accept(Object, Object)}.
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 *
 * @see ContinuableConsumer
 */
@FunctionalInterface
public interface ContinuableBiConsumer<T, U> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     */
    @continuable void accept(T t, U u);

    /**
     * Returns a composed {@code ContinuableBiConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code ContinuableBiConsumer} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default ContinuableBiConsumer<T, U> andThen(ContinuableBiConsumer<? super T, ? super U> after) {
        Objects.requireNonNull(after);
        return new ContinuableBiConsumer<T, U>() {
            @Override
            public void accept(T t, U u) {
                ContinuableBiConsumer.this.accept(t, u);
                after.accept(t, u);
            }
        };
    }
}