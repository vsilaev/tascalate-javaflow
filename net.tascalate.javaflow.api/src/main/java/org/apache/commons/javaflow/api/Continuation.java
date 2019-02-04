/**
 * ﻿Original work: copyright 1999-2004 The Apache Software Foundation
 * (http://www.apache.org/)
 *
 * This project is based on the work licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Modified work: copyright 2013-2019 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.api;

import java.io.Serializable;

import org.apache.commons.javaflow.core.ReflectionUtils;
import org.apache.commons.javaflow.core.ResumeParameter;
import org.apache.commons.javaflow.core.StackRecorder;
import org.apache.commons.javaflow.core.SuspendResult;
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
abstract public class Continuation implements Serializable {

    private static final Log log = LogFactory.getLog(Continuation.class);
    private static final long serialVersionUID = 3L;
    
    private final Object value;
    
    final StackRecorder stackRecorder;
    
    /**
     * Create a new continuation, which continue a previous continuation.
     */
    Continuation(StackRecorder stackRecorder, Object value) {
        this.stackRecorder = stackRecorder;
        this.value = value;
    }


    /**
     * get the current context.
     *
     * <p>
     * This method returns the same context object given to {@link #startWith(Runnable, Object)}
     * or {@link #resume(Object)}.
     *
     * <p>
     * A different context can be used for each run of a continuation, so
     * this mechanism can be used to associate some state with each execution.
     *
     * @return
     *      Currently associated continuation context
     * @throws 
     *      NullPointerException if this method is invoked outside {@link #startWith(Runnable, Object)}
     *      or {@link #resume(Object)} .
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
     * it's {@link #resume() resumed}.</p>
     * 
     * @param target
     *      The object whose <tt>run</tt> method will be executed. 
     * 
     * @return
     *      always return a non-null valid object.
     */
    public static Continuation startSuspendedWith(Runnable target) {
        return startSuspendedWith(target, false);
    }
    
    /**
     * Creates a new {@link Continuation} object from the specified {@link Runnable}
     * object.
     *
     * <p>
     * Unlike the {@link #startWith(Runnable)} method, this method doesn't actually
     * execute the <tt>Runnable</tt> object. It will be executed when
     * it's {@link #resume() resumed}.</p>
     * 
     * @param target
     *      The object whose <tt>run</tt> method will be executed. 
     * @param singleShot
     *      If true then continuation constructed is performance-optimized but 
     *      may be resumed only once. Otherwise "multi-shot" continuation is created that may 
     *      be resumed multiple times. 
     * @return
     *      always return a non-null valid object.
     */
    public static Continuation startSuspendedWith(Runnable target, boolean singleShot) {
        if(target == null) {
            throw new IllegalArgumentException("target is null");
        }
        StackRecorder stackRecorder = new StackRecorder(target);
        return singleShot ? new SingleShotContinuation(stackRecorder, null) : new MultiShotContinuation(stackRecorder, null);
    }    

    /**
     * Starts executing the specified {@link Runnable} object in an environment
     * that allows {@link Continuation#suspend()}.
     *
     * <p>
     * This is a short hand for <tt>startWith(target,null)</tt>.
     * 
     * @param target
     *      The object whose <tt>run</tt> method will be executed.  
     * @return
     *      Continuation object if runnable supplied is supended, otherwise <code>null</code>
     * @see #startWith(Runnable, Object)
     */
    public static Continuation startWith(Runnable target) {
        return startWith(target, false);
    }
    
    /**
     * Starts executing the specified {@link Runnable} object in an environment
     * that allows {@link Continuation#suspend()}.
     *
     * <p>
     * This is a short hand for <tt>startWith(target,null)</tt>.
     * 
     * @param target
     *      The object whose <tt>run</tt> method will be executed.
     * @param singleShot
     *      If true then continuation constructed is performance-optimized but 
     *      may be resumed only once. Otherwise "multi-shot" continuation is created that may 
     *      be resumed multiple times.       
     * @return
     *      Continuation object if runnable supplied is supended, otherwise <code>null</code>
     * @see #startWith(Runnable, Object)
     */
    public static Continuation startWith(Runnable target, boolean singleShot) {
        return startWith(target, null, singleShot);
    }

    /**
     * Starts executing the specified {@link Runnable} object in an environment
     * that allows {@link Continuation#suspend()}.
     *
     * This method blocks until the continuation suspends or completes.
     *
     * @param target
     *      The object whose <tt>run</tt> method will be executed.
     * @param context
     *      This value can be obtained from {@link #getContext()} until this method returns.
     *      Can be null.
     * @return
     *      If the execution completes and there's nothing more to continue, return null.
     *      Otherwise, the execution has been {@link #suspend() suspended}, in which case
     *      a new non-null continuation is returned.
     * @see #getContext()
     */
    public static Continuation startWith(Runnable target, Object context) {
        return startWith(target, context, false);
    }
    
    /**
     * Starts executing the specified {@link Runnable} object in an environment
     * that allows {@link Continuation#suspend()}.
     *
     * This method blocks until the continuation suspends or completes.
     *
     * @param target
     *      The object whose <tt>run</tt> method will be executed.
     * @param context
     *      This value can be obtained from {@link #getContext()} until this method returns.
     *      Can be null.
     * @param singleShot
     *      If true then continuation constructed is performance-optimized but 
     *      may be resumed only once. Otherwise "multi-shot" continuation is created that may 
     *      be resumed multiple times.      
     * @return
     *      If the execution completes and there's nothing more to continue, return null.
     *      Otherwise, the execution has been {@link #suspend() suspended}, in which case
     *      a new non-null continuation is returned.
     * @see #getContext()
     */
    public static Continuation startWith(Runnable target, Object context, boolean singleShot) {
        if (log.isDebugEnabled()) {
            log.debug("starting new flow from " + ReflectionUtils.descriptionOfObject(target));
        }

        return startSuspendedWith(target, singleShot).resume(context);
    }    

    /**
     * This method is deprecated, please use {@link #resume()} instead
     * 
     * Resumes the execution of the specified continuation from where it's left off.
     *
     * <p>
     * This is a short hand for <tt>continueWith(resumed,null)</tt>.
     * 
     * @param continuation
     *      The suspended continuation to be resumed. Must not be null.
     * 
     * @return
     *      If the execution completes and there's nothing more to continue, return null.
     *      Otherwise, the execution has been {@link #suspend() suspended}, in which case
     *      a new non-null continuation is returned.
     *
     * @see #continueWith(Continuation, Object)
     * 
     * @deprecated
     */
    public static Continuation continueWith(Continuation continuation) {
        return continueWith(continuation, (Object)null);
    }

    /**
     * This method is deprecated, please use {@link #resume(Object)} instead
     * 
     * Resumes the execution of the specified continuation from where it's left off
     * and creates a new continuation representing the new state.
     *
     * This method blocks until the continuation suspends or completes.
     *
     * @param continuation
     *      The suspended continuation to be resumed. Must not be null.
     *      
     * @param value
     *      The value to be returned as a result form {@link #suspend() Continuation.suspend()} call or 
     *      from {@link #getContext()} until this method returns. Can be null.
     * @return
     *      If the execution completes and there's nothing more to continue, return null.
     *      Otherwise, the execution has been {@link #suspend() suspended}, in which case
     *      a new non-null continuation is returned.
     * @see #getContext() 
     * @see #suspend()
     * 
     * @deprecated
     */
    public static Continuation continueWith(Continuation continuation, Object value) {
        if (continuation == null) {
            throw new IllegalArgumentException("continuation parameter must not be null.");
        }

        return continuation.resume(value);
    }
    
    /**
     * Resumes the execution of the specified continuation from where it's left off.
     *
     * <p>
     * This is a short hand for <tt>resume(null)</tt>.
     * 
     * @return
     *      If the execution completes and there's nothing more to continue, return null.
     *      Otherwise, the execution has been {@link #suspend() suspended}, in which case
     *      a new non-null continuation is returned.
     *
     * @see #resume(Object)
     * 
     */    
    public Continuation resume() {
        return resume(null);
    }
    
    /**
     * 
     * Resumes the execution of the specified continuation from where it's left off
     * and creates a new continuation representing the new state.
     *
     * This method blocks until the continuation suspends or completes.
     *
     * @param value
     *      The value to be returned as a result form {@link #suspend() Continuation.suspend()} call or 
     *      from {@link #getContext()} until this method returns. Can be null.
     * @return
     *      If the execution completes and there's nothing more to continue, return null.
     *      Otherwise, the execution has been {@link #suspend() suspended}, in which case
     *      a new non-null continuation is returned.
     * @see #getContext()
     * @see #suspend()
     * 
     */    
    public Continuation resume(Object value) {
        return resumeWith(ResumeParameter.value(value));
    }
    
    /**
     * Abnormally terminates the suspended call chain represented by this continuation.
     * <p>Use this method to execute any clean-up code of suspended methods (<code>finally</code> blocks)
     * when there is no need to {@link #resume()} the continuation.</p>
     */
    public void terminate() {
        resumeWith(ResumeParameter.exit());
    }

    /**
     * Check if captured continuation is serializable
     * @return
     *      true if all variables on captured stack and runnable supplied are serializeble, false otherwise.  
     */
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
        return value;
    }
    
    /**
     * <p>View this continuation as a "multi-shot" continuation that may be resumed multiple times.
     * <p>Conversion to the multi-shot continuation is not always possible, i.e. already resumed
     * single-shot continuation may not be converted to the multi-shot variant.  
     * 
     * @return self if this is already a multi-shot continuation or a newly constructed 
     * multi-shot continuation
     */
    abstract public Continuation multiShot();
    
    /**
     * <p>View this continuation as a performance-optimized continuation that may be resumed only once.
     * <p>Conversion to the single-shot continuation is always possible
     * 
     * @return self if this is already a single-shot continuation or a newly constructed single-shot continuation
     */
    abstract public Continuation singleShot();
    
    /**
     * Stops the running continuation.
     *
     * <p>
     * This method can be only called inside {@link #resume} or {@link #startWith} methods.
     * When called, the thread returns from the above methods with a new {@link Continuation}
     * object that captures the thread state.
     *
     * @return
     *      The value to be returned to suspended code after continuation is resumed.
     *      The value is passed from the client code via {@link #resume(Object)}
     *      and is identical to value returned by {@link #getContext}.
     *      
     * @throws IllegalStateException
     *      if this method is called outside the {@link #resume} or {@link #startWith} methods.
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
     * object that captures the thread state and with {@link #value()} equals to parameter passed.
     *
     * @param value 
     *      The intermediate result yielded by suspended continuations
     *      The value may be accessed via {@link #value()} method of continuation returned
     *
     * @return
     *      The value to be returned to suspended code after continuation is resumed.
     *      The value is passed from the client code via @link #continueWith(Continuation, Object)
     *      and is identical to value returned by {@link #getContext}.
     *      
     * @throws IllegalStateException
     *      if this method is called outside the {@link #continueWith} or {@link #startWith} methods.
     */    
    public static Object suspend(Object value) {
        return StackRecorder.suspend(SuspendResult.valueOf(value));
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
        StackRecorder.exit();
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
        StackRecorder.suspend(SuspendResult.AGAIN);
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
        StackRecorder.suspend(SuspendResult.CANCEL);
    }

    @Override
    public String toString() {
        return "Continuation@" + hashCode() + "/" + ReflectionUtils.getClassLoaderName(this);
    }
    
    abstract protected Continuation resumeWith(ResumeParameter param);
    
    static final class MultiShotContinuation extends Continuation {
        private static final long serialVersionUID = 1L;

        MultiShotContinuation(StackRecorder stackRecorder, Object value) {
            super(stackRecorder, value);
        }
        
        @Override
        public Continuation multiShot() {
            return this;
        }
        
        @Override
        public Continuation singleShot() {
            return new SingleShotContinuation(new StackRecorder(stackRecorder), value());
        }
        
        @Override
        protected Continuation resumeWith(ResumeParameter param) {
            if (log.isDebugEnabled()) {
                log.debug("continueing with continuation " + ReflectionUtils.descriptionOfObject(this));
            }
            while(true) {
                StackRecorder nextStackRecorder = new StackRecorder(stackRecorder);
                SuspendResult result = nextStackRecorder.execute(param);
                if (SuspendResult.EXIT == result) {
                    // no more thing to continue
                    return null;
                } else if (SuspendResult.CANCEL == result) {
                    // return immediately with itself
                    return this;
                } else if (SuspendResult.AGAIN == result) {
                    // re-execute immediately
                    continue;
                }
                
                return new MultiShotContinuation(nextStackRecorder, result.value());
            } 
        }
    }
    
    static final class SingleShotContinuation extends Continuation {
        private static final long serialVersionUID = 1L;
        private boolean isResumed = false;

        SingleShotContinuation(StackRecorder stackRecorder, Object value) {
            super(stackRecorder, value);
        }
        
        @Override
        public Continuation multiShot() {
            if (isResumed) {
               throw new IllegalStateException("Single-shot continuation may not be converted to multi-shot after resume"); 
            }
            return new MultiShotContinuation(new StackRecorder(stackRecorder), value());
        }
        
        @Override
        public Continuation singleShot() {
            return this;
        }
        
        @Override
        protected Continuation resumeWith(ResumeParameter param) {
            if (log.isDebugEnabled()) {
                log.debug("continueing with continuation " + ReflectionUtils.descriptionOfObject(this));
            }
            if (isResumed) {
                if (param == ResumeParameter.exit()) {
                    return null;
                } else {
                    throw new IllegalStateException("Single-shot continuation may be resumed only once");
                }
            }
            isResumed = true;
            StackRecorder nextStackRecorder = stackRecorder; // Use existing one, don't copy
            SuspendResult result = nextStackRecorder.execute(param);
            if (SuspendResult.EXIT == result) {
                // no more thing to continue
                return null;
            } else if (SuspendResult.CANCEL == result) {
                // return immediately with null -- stack is no longer valid
                return null;
            } else if (SuspendResult.AGAIN == result) {
                // invalid operation for exclusive continuation
                throw new IllegalStateException("Single-shot continuation may not be re-tried");
            }
            
            return new SingleShotContinuation(nextStackRecorder, result.value());
        }
    }

}
