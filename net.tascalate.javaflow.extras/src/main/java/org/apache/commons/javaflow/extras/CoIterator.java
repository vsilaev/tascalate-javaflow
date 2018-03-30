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
 * @param <E>
 * Type of objects returned by the iterator
 */
public class CoIterator<E> implements ClosableIterator<E>, Serializable {
    private static final long serialVersionUID = 1L;
    
    private boolean advance;
    private Continuation cc;

    /**
     * Iterator constructor
     * 
     * @param code
     * Continuable code that yields multiple results via call to
     * {@link Continuation#suspend(Object)}
     */
    public CoIterator(Runnable code) {
        cc = Continuation.startSuspendedWith(code);
        advance = null != cc;
    }

    /**
     * Iterator constructor
     * 
     * @param code
     * {@link ContinuableRunnable} code that yields multiple results via call to
     * {@link Continuation#suspend(Object)}
     */
    public CoIterator(ContinuableRunnable code) {
        this(ContinuationSupport.toRunnable(code));
    }

    /**
     * <p>Iterator constructor
     * <p>Valued returned by this iterator will be results these are yielded via call to 
     * {@link Continuation#suspend(Object)}, i.e. cc.value() is not included
     * 
     * @param cc
     * Current {@link Continuation} to start iteration from. 
     */
    public CoIterator(Continuation cc) {
        this(cc, false);
    }
    
    /**
     * <p>Iterator constructor
     * <p>Valued returned by this iterator will be results these are yielded via call to 
     * {@link Continuation#suspend(Object)}, i.e. cc.value() is not included unless 
     * <code>useCurrentValue</code> is true
     *  
     * @param cc
     * Current {@link Continuation} to start iteration from.
     * 
     * @param useCurrentValue
     * Should the value of the supplied continuation be used as a first returned value 
     */
    public CoIterator(Continuation cc, boolean useCurrentValue) {
        this.cc = cc;
        advance = !useCurrentValue && null != cc;
    }
    
    public boolean hasNext() {
        advanceIfNecessary();
        return cc != null;
    }

    public E next() {
        advanceIfNecessary();

        if (cc == null)
            throw new NoSuchElementException();

        @SuppressWarnings("unchecked")
        E result = (E)cc.value();
        advance = true;

        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void close() {
        try {
            cc.terminate();
        } finally {
            cc = null;
            advance = false;
        }
    }
    
    protected void advanceIfNecessary() {
        if (advance) {
            cc = cc.resume();
        }
        advance = false;
    }
}
