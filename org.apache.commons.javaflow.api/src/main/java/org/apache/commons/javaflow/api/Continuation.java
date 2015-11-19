/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.javaflow.api;

import java.io.Serializable;

import org.apache.commons.javaflow.core.ContinuationDeath;
import org.apache.commons.javaflow.core.ReflectionUtils;
import org.apache.commons.javaflow.core.StackRecorder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Snapshot of a thread execution state.
 *
 * <p>
 * A {@link Continuation} object is an immutable object that captures everything in
 * the Java stack. This includes
 * (1) current instruction pointer,
 * (2) return addresses, and
 * (3) local variables.
 *
 * <p>
 * <tt>Continuation</tt> objects are used to restore the captured execution states
 * later.
 * 
 */
public final class Continuation implements Serializable {

    private static final Log log = LogFactory.getLog(Continuation.class);
    private static final long serialVersionUID = 2L;
    
    private final StackRecorder stackRecorder;

    /**
     * Create a new continuation, which continue a previous continuation.
     */
    private Continuation( final StackRecorder pStackRecorder ) {
        stackRecorder = pStackRecorder;
    }


    /**
     * get the current context.
     *
     * <p>
     * This method returns the same context object given to {@link #startWith(Runnable, Object)}
     * or {@link #continueWith(Continuation, Object)}.
     *
     * <p>
     * A different context can be used for each run of a continuation, so
     * this mechanism can be used to associate some state with each execution.
     *
     * @return
     *      null if this method is invoked outside {@link #startWith(Runnable, Object)}
     *      or {@link #continueWith(Continuation, Object)} .
     */
    public static Object getContext() {
        return StackRecorder.get().getContext();
    }

    /**
     * Creates a new {@link Continuation} object from the specified {@link Runnable}
     * object.
     *
     * <p>
     * Unlike the {@link #startWith(Runnable)} method, this method doesn't actually
     * execute the <tt>Runnable</tt> object. It will be executed when
     * it's {@link #continueWith(Continuation) continued}.
     * 
     * @return
     *      always return a non-null valid object.
     */
    public static Continuation startSuspendedWith( final Runnable pTarget ) {
        return new Continuation(new StackRecorder(pTarget));
    }

    /**
     * Starts executing the specified {@link Runnable} object in an environment
     * that allows {@link Continuation#suspend()}.
     *
     * <p>
     * This is a short hand for <tt>startWith(target,null)</tt>.
     *
     * @see #startWith(Runnable, Object).
     */
    public static Continuation startWith( final Runnable pTarget ) {
        return startWith(pTarget, null);
    }

    /**
     * Starts executing the specified {@link Runnable} object in an environment
     * that allows {@link Continuation#suspend()}.
     *
     * This method blocks until the continuation suspends or completes.
     *
     * @param pTarget
     *      The object whose <tt>run</tt> method will be executed.
     * @param pContext
     *      This value can be obtained from {@link #getContext()} until this method returns.
     *      Can be null.
     * @return
     *      If the execution completes and there's nothing more to continue, return null.
     *      Otherwise, the execution has been {@link #suspend() suspended}, in which case
     *      a new non-null continuation is returned.
     * @see #getContext()
     */
    public static Continuation startWith( final Runnable pTarget, final Object pContext ) {
        if(pTarget == null) {
            throw new IllegalArgumentException("target is null");
        }

        if (log.isDebugEnabled()) {
        	log.debug("starting new flow from " + ReflectionUtils.getClassName(pTarget) + "/" + ReflectionUtils.getClassLoaderName(pTarget));
        }

        return continueWith(new Continuation(new StackRecorder(pTarget)), pContext);
    }

    /**
     * Resumes the execution of the specified continuation from where it's left off.
     *
     * <p>
     * This is a short hand for <tt>continueWith(resumed,null)</tt>.
     *
     * @see #continueWith(Continuation, Object)
     */
    public static Continuation continueWith(final Continuation pOldContinuation) {
        return continueWith(pOldContinuation, null);
    }

    /**
     * Resumes the execution of the specified continuation from where it's left off
     * and creates a new continuation representing the new state.
     *
     * This method blocks until the continuation suspends or completes.
     *
     * @param pOldContinuation
     *      The resumed continuation to be executed. Must not be null.
     * @param pContext
     *      This value can be obtained from {@link #getContext()} until this method returns.
     *      Can be null.
     * @return
     *      If the execution completes and there's nothing more to continue, return null.
     *      Otherwise, the execution has been {@link #suspend() suspended}, in which case
     *      a new non-null continuation is returned.
     * @see #getContext()
     */
    public static Continuation continueWith(final Continuation pOldContinuation, final Object pContext) {
        if (pOldContinuation == null) {
            throw new IllegalArgumentException("continuation parameter must not be null.");
        }

        if (log.isDebugEnabled()) {
        	log.debug("continueing with continuation " + ReflectionUtils.getClassName(pOldContinuation) + "/" + ReflectionUtils.getClassLoaderName(pOldContinuation));
        }

        while(true) {
            try {
                StackRecorder pStackRecorder =
                    new StackRecorder(pOldContinuation.stackRecorder).execute(pContext);
                if(pStackRecorder == null) {
                    return null;
                } else {
                    return new Continuation(pStackRecorder);
                }
            } catch (ContinuationDeath e) {
                if(e.mode.equals(ContinuationDeath.MODE_AGAIN))
                    continue;       // re-execute immediately
                if(e.mode.equals(ContinuationDeath.MODE_EXIT))
                    return null;    // no more thing to continue
                if(e.mode.equals(ContinuationDeath.MODE_CANCEL))
                    return pOldContinuation;
                throw new IllegalStateException("Illegal mode "+e.mode);
            }
        }
    }

    public boolean isSerializable() {
        return stackRecorder.isSerializable();
    }
    
    /**
     * Accessor for value yielded by continuation  
     * 
     * @return
     *      The latest value yielded by suspended continuation.
     *      The value is passed from the continuation to the client code via {@link #suspend(Object)}
     */
    public Object value() {
    	return stackRecorder.value;
    }
    
    /**
     * Stops the running continuation.
     *
     * <p>
     * This method can be only called inside {@link #continueWith} or {@link #startWith} methods.
     * When called, the thread returns from the above methods with a new {@link Continuation}
     * object that captures the thread state.
     *
     * @return
     *      The value to be returned to suspended code after continuation is resumed.
     *      The value is passed from the client code via @link #continueWith(Continuation, Object)
     *      and is identical to value returned by {@link #getContext}.
     *      
     * @throws IllegalStateException
     *      if this method is called outside the {@link #continueWith} or {@link #startWith} methods.
     */
    public static Object suspend() {
    	return suspend(null);
    }
    
    /**
     * Stops the running continuation.
     *
     * <p>
     * This method can be only called inside {@link #continueWith} or {@link #startWith} methods.
     * When called, the thread returns from the above methods with a new {@link Continuation}
     * object that captures the thread state and with {@link #value} equals to parameter passed.
     *
     * @param value 
     *      The intermediate result yielded by suspended continuations
     *      The value may be accessed via {@link #value} method of continuation returned
     *
     * @return
     *      The value to be returned to suspended code after continuation is resumed.
     *      The value is passed from the client code via @link #continueWith(Continuation, Object)
     *      and is identical to value returned by {@link #getContext}.
     *      
     * @throws IllegalStateException
     *      if this method is called outside the {@link #continueWith} or {@link #startWith} methods.
     */    
    public static Object suspend(final Object value) {
        return StackRecorder.suspend(value);
    }


    /**
     * Completes the execution of the running continuation.
     *
     * <p>
     * This method can be only called inside {@link #continueWith} or {@link #startWith} methods.
     * When called, the thread returns from the above methods with null,
     * indicating that there's nothing more to continue.
     *
     * <p>
     * This method is similiar to how {@link System#exit(int)} works for JVM.
     */
    public static void exit() {
        throw new ContinuationDeath(ContinuationDeath.MODE_EXIT);
    }

    /**
     * Jumps to where the execution was resumed.
     *
     * <p>
     * This method can be only called inside {@link #continueWith} or {@link #startWith} methods.
     * When called, the execution jumps to where it was resumed
     * (if the execution has never resumed before, from the beginning
     * of {@link Runnable#run()}.)
     *
     * <p>
     * Consider the following example:
     *
     * <pre>
     * Continuation.suspend();
     * System.out.println("resumed");
     *
     * r = new Random().nextInt(5);
     * if(r!=0) {
     *   System.out.println("do it again");
     *   Continuation.again();
     * }
     *
     * System.out.println("done");
     * </pre>
     *
     * <p>
     * This program produces an output like this (the exact number of
     * 'do it again' depends on each execution, as it's random.)
     *
     * <pre>
     * resumed
     * do it again
     * resumed
     * do it again
     * resumed
     * do it again
     * resumed
     * done
     * </pre>
     *
     * <p>
     * The calling {@link Continuation#startWith(Runnable)} method and
     * {@link Continuation#continueWith(Continuation)} method does not
     * return when a program running inside uses this method.
     */
    public static void again() {
        throw new ContinuationDeath(ContinuationDeath.MODE_AGAIN);
    }

    /**
     * Jumps to where the execution was resumed, and suspend execution.
     *
     * <p>
     * This method almost works like the {@link #again()} method,
     * but instead of re-executing, this method first suspends the execution.
     *
     * <p>
     * Therefore,
     * the calling {@link Continuation#startWith(Runnable)} method and
     * {@link Continuation#continueWith(Continuation)} method
     * return when a program running inside uses this method.
     */
    public static void cancel() {
        throw new ContinuationDeath(ContinuationDeath.MODE_CANCEL);
    }

    public String toString() {
        return "Continuation@" + hashCode() + "/" + ReflectionUtils.getClassLoaderName(this);
    }
}
