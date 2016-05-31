package org.apache.commons.javaflow.extras;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.javaflow.api.Continuation;

/**
 * 
 * @author Valery Silaev
 *
 * Read-only iterator over multiple results returned by continuation passed 
 * The iterator works similar to <i>yield</i> keyword found in other languages 
 * 
 * Usage:
 * <code>
 * final Runnable code = instantiateContinuableCode();
 * for (final Iterator&lt;String&gt; i = new CoIterator&lt;String&gt;(code); i.hasNext(); ) {
 *   final String line = i.next();
 *   System.out.println( line );
 * } 
 * </code>
 * 
 * Also CoIterator created iterates over multiple values yielded by continuation, 
 * it's impossible to pass value back to continuation from client code; the result of 
 * {@link Continuation#suspend(Object)} in continuation will be always <i>null</i> 
 * 
 * @param <T>
 * Type of objects returned by the iterator
 */
public class CoIterator<T> implements Iterator<T> {
	private boolean advance = true;
	private Continuation cc;
	
	/**
	 * Iterator constructor
	 * 
	 * @param code
	 * Continuable code that yields multiple results via call to
	 * {@link Continuation#suspend(Object)}
	 */
	public CoIterator(final Runnable code) {
		cc = Continuation.startSuspendedWith(code);
	}
	
	public boolean hasNext() {
		advanceIfNecessary();
		return cc != null;
	}
	
	public T next() {
		advanceIfNecessary();
		
		if (cc == null)
			throw new NoSuchElementException();
		
		@SuppressWarnings("unchecked")
		final T result = (T)cc.value();
		advance = true;

		return result;
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	protected void advanceIfNecessary() {
		if (advance)
			cc = cc.resume();
		advance = false;
	}
}
