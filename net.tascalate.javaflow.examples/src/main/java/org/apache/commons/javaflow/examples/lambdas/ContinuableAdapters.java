package org.apache.commons.javaflow.examples.lambdas;

import org.apache.commons.javaflow.extras.ContinuableConsumer;
import org.apache.commons.javaflow.extras.ContinuableRunnable;

public class ContinuableAdapters {

    public static ContinuableRunnable exec(ContinuableRunnable o) {
        return o;
    }

    public static <T> ContinuableConsumer<T> accept(ContinuableConsumer<T> o) {
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
