/**
 * Copyright 2013-2017 Valery Silaev (http://vsilaev.com)
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

import static org.apache.commons.javaflow.extras.ContinuationSupport.toRunnable;

import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

final public class Continuations {
    
    private Continuations() {
    }

    /**
     * Creates a suspended continuation, {@link ContinuableRunnable} is not started
     * @param o a continuable code block 
     * @return the continuation, suspended before code starts
     */
    public static Continuation create(ContinuableRunnable o) {
        return Continuation.startSuspendedWith(toRunnable(o));
    }

    /**
     * Starts {@link ContinuableRunnable} code block and returns a continuation, 
     * corresponding to the first {@link Continuation#suspend()} call inside this code block 
     * (incl. nested continuable method calls), if any exists. Returns null if the code
     * is not suspended.
     * 
     * @param o a continuable code block
     * @param ctx an initial argument for the continuable code
     * @return the first continuation suspended
     */
    public static Continuation start(ContinuableRunnable o, Object ctx) {
        return Continuation.startWith(toRunnable(o), ctx);
    }

    /**
     * Starts {@link ContinuableRunnable} code block and returns a continuation, 
     * corresponding to the first {@link Continuation#suspend()} call inside this code block 
     * (incl. nested continuable method calls), if any exists. Returns null if the code
     * is not suspended.
     * 
     * @param o a continuable code block
     * @return the first continuation suspended
     */
    public static Continuation start(ContinuableRunnable o) {
        return Continuation.startWith(toRunnable(o));
    }

    /**
     * Executes the suspended continuation from the point specified till the end 
     * of the corresponding code block and performs a potentially suspendable action 
     * on each value yielded.
     * 
     * @param continuation a continuation to resume a code block that yields multiple results
     * @param action a continuable action to perform on the values yielded
     */  
    public @continuable static void execute(Continuation continuation, ContinuableConsumer<? super Object> action) {
        execute(continuation, Object.class, action);
    }
    
    /**
     * Fully executes the continuable code block and performs a potentially suspendable 
     * action on each value yielded.
     * 
     * @param generator a continuable code block that yields multiple results
     * @param action a continuable action to perform on the values yielded
     */
    public @continuable static void execute(ContinuableRunnable generator, ContinuableConsumer<? super Object> action) {
        execute(generator, Object.class, action);
    }
    
    
    /**
     * Executes the suspended continuation from the point specified till the end 
     * of the corresponding code block and performs a potentially suspendable action 
     * on each value yielded.
     * 
     * @param <T> a type of values  
     * @param continuation a continuation to resume a code block that yields multiple results
     * @param valueType a type of the values yielded from code block
     * @param action a continuable action to perform on the values yielded
     */
    public @continuable static <T> void execute(Continuation continuation, Class<T> valueType, ContinuableConsumer<? super T> action) {
        forEach(()-> new CoIterator<>(continuation), action);
    }
    
    /**
     * Fully executes the continuable code block and performs a potentially suspendable 
     * action on each value yielded.
     * 
     * @param <T> a type of values 
     * @param generator a continuable code block that yields multiple results
     * @param valueType a type of the values yielded from code block
     * @param action a continuable action to perform on the values yielded
     */
    public @continuable static <T> void execute(ContinuableRunnable generator, Class<T> valueType, ContinuableConsumer<? super T> action) {
        forEach(()-> new CoIterator<>(generator), action);
    }

    private @continuable static <T> void execute(Supplier<CoIterator<T>> iteratorProvider, Class<T> valueType, ContinuableConsumer<? super T> action) {
        try (CoIterator<T> iterator = iteratorProvider.get()) {
            forEach(iterator, action);
        }
    }
    

    /**
     * Performs an continuable action for each element of the {@link Stream} supplied.
     *
     * <p>This is a terminal operation that should be used instead of 
     * {@link Stream#forEach(java.util.function.Consumer)} with continuable code.
     * 
     * @param <T> a type of elements 
     * @param stream the stream to perform an action on
     * @param action a continuable action to perform on the elements
     */    
    public @continuable static <T> void forEach(Stream<T> stream, ContinuableConsumer<? super T> action) {
        forEach(stream.iterator(), action);
    }

    /**
     * Performs an continuable action for each element of the {@link Iterable} supplied.
     *
     * <p>This is a convenient functional replacement for the Java 7 For-Each Loop
     * over {@link Iterable}.
     * 
     * @param <T> a type of elements 
     * @param iterable the iterable to perform an action on
     * @param action a continuable action to perform on the elements
     */   
    public @continuable static <T> void forEach(Iterable<T> iterable, ContinuableConsumer<? super T> action) {
        forEach(iterable.iterator(), action);
    }

    /**
     * Performs an continuable action for each element of the {@link Iterator} supplied.
     *
     * <p>This is a convenient functional replacement for the classic Java While Loop 
     * over {@link Iterator}.
     * 
     * @param <T> a type of elements
     * @param iterator the iterator to perform an action on
     * @param action a continuable action to perform on the elements
     */ 
    public @continuable static <T> void forEach(Iterator<T> iterator, ContinuableConsumer<? super T> action) {
        while (iterator.hasNext()) {
            action.accept(iterator.next());
        }
    }


}
