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
package org.apache.commons.javaflow.examples.lambdas;

import org.apache.commons.javaflow.extras.ContinuableConsumer;

public class ContinuableAdapters {

    public static MyContinuableRunnable runnable(MyContinuableRunnable o) {
        return o;
    }

    public static <T> ContinuableConsumer<T> consumer(ContinuableConsumer<T> o) {
        return o;
    }

    /*
    interface ContinuableConsumer<T> extends Consumer<T> {
        @continuable void accept(T t);

        default ContinuableConsumer<T> andThen(ContinuableConsumer<? super T> after) {
            return t -> {
                ContinuableConsumer.this.accept(t);
                after.accept(t);
            };
        }

        // Hand-made bridge method
        default ContinuableConsumer<T> andThen(Consumer<? super T> after) {
            return andThen((ContinuableConsumer<? super T>)after);
        }
    }
    */
}
