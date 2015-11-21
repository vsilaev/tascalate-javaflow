package org.apache.commons.javaflow.examples.lambdas;

import java.util.Iterator;
import java.util.function.Consumer;

import org.apache.commons.javaflow.api.ccs;
import org.apache.commons.javaflow.api.continuable;

public class ContinuableAdapters {

	public static <T> cConsumer<T> apply(final cConsumer<T> o) {
		return o;
	}
	
	public static <T> cIterable<T> from(final Iterable<T> o) {
		return new cIterable<T>() {
			public Iterator<T> iterator() { return o.iterator(); }
		};
	}
	
	
	interface cConsumer<T> extends Consumer<T> {
		@continuable void accept(T t);
		
		default cConsumer<T> andThen(Consumer<? super T> after) {
			return new cConsumer<T>() {
				public void accept(T t) {
					@ccs Consumer<? super T> next = after; 
					cConsumer.this.accept(t);
					next.accept(t);
				}
			};
	    }
	}
	
	interface cIterable<T> extends Iterable<T> {
		default @continuable void forEach(@ccs Consumer<? super T> action) {
			for (Iterator<T> it = iterator(); it.hasNext(); ) {
				T v = it.next();
				action.accept(v);
			}
		}
	}
	
}
