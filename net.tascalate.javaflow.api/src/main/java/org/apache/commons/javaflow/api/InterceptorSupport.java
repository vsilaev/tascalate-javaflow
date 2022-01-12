/**
 * ﻿Copyright 2013-2022 Valery Silaev (http://vsilaev.com)
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

import org.apache.commons.javaflow.core.Skip;
import org.apache.commons.javaflow.core.StackRecorder;

public final class InterceptorSupport {
    private InterceptorSupport() {}
    
    public static boolean isInstrumented(Object target) {
        if (null == target) {
            return false;
        } else {
            return isInstrumented(target.getClass());
        }
    }
    
    public static boolean isInstrumented(Class<?> targetClass) {
        if (null == targetClass) {
            return false;
        } else {
            return null != targetClass.getAnnotation(Skip.class);
        }
    }
    
    public static void beforeExecution(Object actualTarget) {
        // When restoring we should replace target on top of the stack (if any)
        // with the actual target (supplied as argument) from interceptor
        // to balance the effect of non-continuable interceptors call
        // There are might be no reference at all if interceptor is non-continuable
        StackRecorder stackRecorder = StackRecorder.get();
        if (null != stackRecorder && stackRecorder.isRestoring && stackRecorder.hasReference()) {
            stackRecorder.popReference();
            stackRecorder.pushReference(actualTarget);
        }
    }
    
    public static void afterExecution(Object proxiedTarget) {
        // When capturing we should replace target on top of the stack
        // with the proxied target (supplied as argument) expected by the caller 
        // to balance the effect of non-continuable interceptors call
        StackRecorder stackRecorder = StackRecorder.get();
        if (null != stackRecorder && stackRecorder.isCapturing) {
            stackRecorder.popReference();
            stackRecorder.pushReference(proxiedTarget);
        }
    }
}
