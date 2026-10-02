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

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

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
        return DELEGATE;
    }
    
    private static final boolean CHECK_THREAD = Boolean.getBoolean("net.tascalate.javaflow.check-thread"); 
    private static final ScopedContinuationExecutor DELEGATE;
    
    static {
        var majorVersion = Runtime.version().feature();
        boolean useScopedValue;
        if (majorVersion >= 25) {
            useScopedValue = true;
        } else if (majorVersion < 21) {
            useScopedValue = false;
        } else {
            Set<String> args = new HashSet<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
            useScopedValue = args.contains("--enable-preview");
        }
        if (useScopedValue) {
            // Using ScopedValue as scoped continuation executor
            DELEGATE = ScopedValueContinuationExecutor.INSTANCE;
        } else {
            // Using ThreadLocal as scoped continuation executor
            DELEGATE = ThreadLocalContinuationExecutor.INSTANCE;
        }
    }
}
