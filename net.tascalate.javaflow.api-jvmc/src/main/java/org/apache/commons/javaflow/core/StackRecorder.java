/**
 * ﻿Original work: copyright 1999-2004 The Apache Software Foundation
 * (http://www.apache.org/)
 *
 * This project is based on the work licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Modified work: copyright 2013-2025 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.core;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StackRecorder {

    private static final Logger log = LoggerFactory.getLogger(StackRecorder.class);
    
    private static final Runnable NO_OP_RUNNABLE = new Runnable() {
        public void run() {}
    };
    
    static final StackRecorder INVALID = new StackRecorder(NO_OP_RUNNABLE);
    
    private final static jdk.internal.vm.ContinuationScope SCOPE = new jdk.internal.vm.ContinuationScope("javaflow-continuation");
    
    /** Parameter object passed by the client code to continuation during resume */
    private transient ResumeParameter parameter;
    /** Result object passed by the continuation to the client code during suspend */
    private transient SuspendResult result;

    private final Runnable runnable;
    private final jdk.internal.vm.Continuation nativeContinuation;
    /**
     * Creates a new empty {@link StackRecorder} that runs the given target.
     * @param target
     *       a target to run
     */
    public StackRecorder(Runnable target) {
        runnable = target;

        nativeContinuation = new jdk.internal.vm.Continuation(SCOPE, target);
    }

    public static Object suspend(SuspendResult value, Runnable stackOwner) {
        StackRecorder stackRecorder = null;
        if (null != stackOwner) {
            stackRecorder = ((StackOwner)stackOwner).getStack();
        }
        
        return null != stackRecorder ? stackRecorder.suspend0(value) : suspend(value);
    }
    
    public static Object suspend(SuspendResult value) {
        log.debug("suspend()");
        var stackRecorder = get();
        if (stackRecorder == null) {
            throw new IllegalStateException("No continuation is running");
        } else {
            return stackRecorder.suspend0(value);
        }

    }
    
    private Object suspend0(SuspendResult value) {
        result = value;
        jdk.internal.vm.Continuation.yield(SCOPE);

        // flow breaks here, actual return will be executed in resumed continuation
        // return in continuation to be suspended is executed as well but ignored
        
        if (null != parameter) {
            parameter.checkExit();
        }
        
        return parameter.value();
    }

    public SuspendResult execute(final ResumeParameter parameter) {
        if (null == parameter) {
            throw new IllegalArgumentException("ResumeContext parameter may not be null");
        }

        try {
            this.parameter = parameter;

            if (log.isDebugEnabled()) {
                log.debug("Restoring state of " + ReflectionUtils.descriptionOfObject(runnable));
            }
            
            if (runnable instanceof StackOwner) {
                ((StackOwner) runnable).setStack(this);
            }
            
            log.debug("calling runnable");
            PlatformContinuationExecutor.current().runWith(this, nativeContinuation::run);

            if (nativeContinuation.isDone()) {
                return SuspendResult.EXIT;    // nothing more to continue
            } else {
                return result;
            }
        } catch(ContinuationDeath cd) {
            // this isn't an error, so no need to log
            return SuspendResult.EXIT;
        } catch(Error | RuntimeException e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            this.parameter = null;
            this.result = null;
        }                
    }

    public static void exit() {
        throw ContinuationDeath.INSTANCE;
    }
    
    /**
     * Access value supplied to resumed method
     * 
     * @return value passed to the resumed method (may be <code>null</code>)
     * 
     */
    public Object getContext() {
        return null == parameter ? null : parameter.value();
    }

    /**
     * Return the continuation, which is associated to the current thread.
     * @return currently associated continuation stack, or <code>null</code> if invoked outside
     * of continuation context
     */
    public static StackRecorder get() {
        return PlatformContinuationExecutor.current().currentStackRecorder();
    }
    
    public static boolean isExitSignal(Throwable ex) {
        return ContinuationDeath.INSTANCE == ex; 
    }
}
