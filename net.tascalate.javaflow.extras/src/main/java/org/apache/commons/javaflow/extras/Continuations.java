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

import java.io.Serializable;
import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

final public class Continuations {
    private Continuations() {
    }

    public static Continuation create(ContinuableRunnable o) {
        return Continuation.startSuspendedWith(adapt(o));
    }

    public static Continuation start(ContinuableRunnable o, Object ctx) {
        return Continuation.startWith(adapt(o), ctx);
    }

    public static Continuation start(ContinuableRunnable o) {
        return Continuation.startWith(adapt(o));
    }

    public @continuable static <T> void forEach(ContinuableRunnable coroutine, ContinuableConsumer<? super T> action) {
        forEach(new CoIterator<>(coroutine), action);
    }

    public @continuable static <T> void forEach(Stream<T> iterable, ContinuableConsumer<? super T> action) {
        forEach(iterable.iterator(), action);
    }

    public @continuable static <T> void forEach(Iterable<T> iterable, ContinuableConsumer<? super T> action) {
        forEach(iterable.iterator(), action);
    }

    public @continuable static <T> void forEach(Iterator<T> iterator, ContinuableConsumer<? super T> action) {
        while (iterator.hasNext()) {
            action.accept(iterator.next());
        }
    }

    private static Runnable adapt(final ContinuableRunnable code) {
        @SuppressWarnings("serial")
        abstract class ContinuableRunnableAdapter implements Runnable, Serializable {
        }

        return new ContinuableRunnableAdapter() {
            private static final long serialVersionUID = 0L;

            @Override
            public @continuable void run() {
                code.run();
            }
        };
    }

}
