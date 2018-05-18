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
package org.apache.commons.javaflow.api;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
        }
        try {
            final Field field = targetClass.getField("___$$$CONT$$$___");
            return (field.getModifiers() & Modifier.STATIC) != 0;
        } catch (final NoSuchFieldException ex) {
            // It's ok, just report "false" back
        }
        return false;
    }
    
    public static void beforeExecution() {
        // Currently no-op
    }
    
    public static void afterExecution(Object proxiedTarget) {
        StackRecorder stackRecorder = StackRecorder.get();
        
        // When capturing we should replace target on top of the stack
        // with the proxied target (supplied as argument) expected by the caller 
        // to balance the effect of non-continuable interceptors call
        if (stackRecorder != null && stackRecorder.isCapturing) {
            stackRecorder.popReference();
            stackRecorder.pushReference(proxiedTarget);
        }
    }
}
