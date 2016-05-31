package org.apache.commons.javaflow.examples.lambdas;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.extras.ContinuableRunnable;

public class ContinuableAdapters {

    public static ContinuableRunnable exec(ContinuableRunnable o) {
        return o;
    }

    public static <T> ContinuableConsumer<T> accept(ContinuableConsumer<T> o) {
        return o;
    }

    public @continuable static <T> void forEach(Stream<T> o, ContinuableConsumer<? super T> action) {
        forEach(o.iterator(), action);
    }

    public @continuable static <T> void forEach(Iterable<T> o, ContinuableConsumer<? super T> action) {
        forEach(o.iterator(), action);
    }

    public @continuable static <T> void forEach(Iterator<T> it, ContinuableConsumer<? super T> action) {
        while (it.hasNext()) {
            T v = it.next();
            action.accept(v);
        }
    }

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
}
