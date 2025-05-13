/**
 * ﻿Copyright 2013-2025 Valery Silaev (http://vsilaev.com)
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

final class ThreadLocalContinuationExecutor implements ScopedContinuationExecutor {
    
    static final ScopedContinuationExecutor INSTANCE = new ThreadLocalContinuationExecutor();
    
    private static final ThreadLocal<StackRecorder> STACK_RECORDER = new ThreadLocal<StackRecorder>();
    
    private ThreadLocalContinuationExecutor() {
        
    }
    
    public final void runWith(StackRecorder stackRecorder, Runnable code) {
        StackRecorder prevStackRecorder = STACK_RECORDER.get();
        STACK_RECORDER.set(stackRecorder);
        try {
            code.run();
        } finally {
            if (null == prevStackRecorder) {
                STACK_RECORDER.remove();
            } else {
                STACK_RECORDER.set(prevStackRecorder);
            }
        }
    }
    
    public final StackRecorder currentStackRecorder() {
        return STACK_RECORDER.get();
    }
}
