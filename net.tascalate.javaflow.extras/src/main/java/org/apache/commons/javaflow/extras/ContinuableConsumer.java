/**
 * ﻿Copyright 2013-2017 Valery Silaev (http://vsilaev.com)
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
 * Represents an continuable operation that accepts a single input argument and returns no
 * result. Unlike most other functional interfaces, {@code ContinuableConsumer} is expected
 * to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object)}.
 *
 * @param <T> the type of the input to the operation
 *
 */
@FunctionalInterface
public interface ContinuableConsumer<T> {
    
    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    @continuable void accept(T t);
    
    /**
     * Returns a composed {@code ContinuableConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code ContinuableConsumer} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default ContinuableConsumer<T> andThen(ContinuableConsumer<? super T> after) {
        Objects.requireNonNull(after);
        return new ContinuableConsumer<T>() {
            public @continuable void accept(T t) {
                ContinuableConsumer.this.accept(t);
                after.accept(t);
            }
        };
    }

}
