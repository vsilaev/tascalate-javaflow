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

final public class InterceptorSupport {
    private InterceptorSupport() {}
    
    public static boolean isInstrumented(final Object target) {
        if (null == target) {
            return false;
        }
        try {
            final Field field = target.getClass().getField("___$$$CONT$$$___");
            return (field.getModifiers() & Modifier.STATIC) != 0;
        } catch (final NoSuchFieldException ex) {
            // It's ok, just report "false" back
        }
        return false;
    }
    
    public static Object beforeExecution() {
        StackRecorder stackRecorder = StackRecorder.get();

        // When restoring we should remove element from the stack
        // to balance the effect of non-continuable interceptors call
        // The element removed is a target behind interceptors
        if (stackRecorder != null && stackRecorder.isRestoring) {
            return stackRecorder.popReference();
        } else {
            return null;
        }
    }
    
    public static void afterExecution(final Object interceptor) {
        StackRecorder stackRecorder = StackRecorder.get();
        
        // When capturing we should place self-reference on the stack
        // to balance the effect of non-continuable interceptors call
        if (stackRecorder != null && stackRecorder.isCapturing) {
            stackRecorder.pushReference(interceptor);
        }
    }
}
