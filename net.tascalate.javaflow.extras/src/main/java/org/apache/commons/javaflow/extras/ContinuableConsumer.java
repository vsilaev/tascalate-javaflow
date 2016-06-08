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
