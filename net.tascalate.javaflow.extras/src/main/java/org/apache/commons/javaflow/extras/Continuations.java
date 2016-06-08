package org.apache.commons.javaflow.extras;

import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

final public class Continuations {
    private Continuations() {}

    public static Continuation create(ContinuableRunnable o) {
        return Continuation.startSuspendedWith(o);
    }

    public static Continuation start(ContinuableRunnable o, Object ctx) {
        return Continuation.startWith(o, ctx);
    }

    public static Continuation start(ContinuableRunnable o) {
        return Continuation.startWith(o);
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
            action.accept( iterator.next() );
        }
    }
}
