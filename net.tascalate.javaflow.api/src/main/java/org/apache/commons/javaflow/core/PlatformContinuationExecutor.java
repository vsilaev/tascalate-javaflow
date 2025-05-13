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

final class PlatformContinuationExecutor {
    private PlatformContinuationExecutor() {
        
    }
    
    static ScopedContinuationExecutor current() {
        if (CHECK_THREAD) {
            Thread currentThread = Thread.currentThread();
            if (currentThread instanceof ScopedContinuationExecutor) {
                return (ScopedContinuationExecutor)currentThread;
            }
        }
        return ThreadLocalContinuationExecutor.INSTANCE;
    }
    
    private static final boolean CHECK_THREAD = Boolean.getBoolean("net.tascalate.javaflow.check-thread"); 
}
