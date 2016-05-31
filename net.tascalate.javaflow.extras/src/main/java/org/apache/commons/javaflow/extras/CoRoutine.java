package org.apache.commons.javaflow.extras;

import java.util.Iterator;

import org.apache.commons.javaflow.api.Continuation;

/**
 * 
 * @author sav
 * 
 * Adapter for continuable block of code to let iterate over multiple results yielded with Java 5 <i>for loop</i>
 *   
 * Usage:
 * <code>
 * final Runnable code = instantiateContinuableCode();
 * for (final String line : new CoRoutine&lt;String&gt;(code) ) {
 *   System.out.println( line );
 * } 
 * </code>
 * 
 * Also CoIterator created by the CoRoutine iterates over multiple values yielded by continuation, 
 * it's impossible to pass value back to continuation from client code; the result of 
 * {@link Continuation#suspend(Object)} in continuation will be always <i>null</i>
 * 
 * @param <T>
 * Type of objects returned by the iterator produced with the CoRoutine 
 */
public class CoRoutine<T> implements Iterable<T> {
	final private Runnable code;
	
	/**
	 * CoRoutine constructor
	 * 
	 * @param code
	 * Continuable code that yields multiple results via call to
	 * {@link Continuation#suspend(Object)}
	 */	
	public CoRoutine(final Runnable code) {
		this.code = code;
	}
	
	public Iterator<T> iterator() {
		return new CoIterator<T>(code);
	}
	
}
