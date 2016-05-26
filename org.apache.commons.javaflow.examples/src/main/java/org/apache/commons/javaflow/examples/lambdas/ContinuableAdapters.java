package org.apache.commons.javaflow.examples.lambdas;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.javaflow.api.ccs;
import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.extras.ContinuableRunnable;

public class ContinuableAdapters {

	public static ContinuableRunnable exec(final ContinuableRunnable o) {
		return o;
	}

	
	public static <T> ContinuableConsumer<T> accept(final ContinuableConsumer<T> o) {
		return o;
	}
	
	public static <T> ContinuableIterable<T> from(final Stream<T> o) {
		return o::iterator;
	}
	
	public static <T> ContinuableIterable<T> from(final Iterable<T> o) {
		return o::iterator;
	}
	
	
	interface ContinuableConsumer<T> extends Consumer<T> {
		@continuable void accept(T t);
		
		default ContinuableConsumer<T> andThen(Consumer<? super T> after) {
			return t -> {
				ContinuableConsumer.this.accept(t);
				// Need explicit check to generate valid bytecode for JavaFlow
				if (after instanceof ContinuableConsumer) {
					((ContinuableConsumer<? super T>)after).accept(t);	
				} else {
					after.accept(t);
				}
			};
	    }
	}
	
	interface ContinuableIterable<T> extends Iterable<T> {
		default @continuable void forEach(@ccs Consumer<? super T> action) {
			for (Iterator<T> it = iterator(); it.hasNext(); ) {
				T v = it.next();
				action.accept(v);
			}
		}
	}
	
}
