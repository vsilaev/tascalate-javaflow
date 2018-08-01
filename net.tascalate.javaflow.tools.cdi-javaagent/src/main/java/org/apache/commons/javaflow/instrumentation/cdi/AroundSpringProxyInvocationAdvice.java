/**
 * ﻿Copyright 2013-2018 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.instrumentation.cdi;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

class AroundSpringProxyInvocationAdvice extends AroundCdiProxyInvocationAdvice {
    protected AroundSpringProxyInvocationAdvice(int api, MethodVisitor mv, int acc, String className, String methodName, String desc) {
        super(api, mv, acc, className, methodName, desc);
    }

    @Override
    protected void loadProxiedInstance() {
        loadThis();
        invokeVirtual(Type.getType(className), GET_TARGET_SOURCE);
        /*
         * May be used instead of above
        invokeInterface(Type.getType("org/springframework/aop/framework/Advised"), GET_TARGET_SOURCE);
        */
        invokeInterface(Type.getType("org/springframework/aop/TargetSource"), GET_TARGET);
    }

    private static final Method GET_TARGET_SOURCE = Method.getMethod("org.springframework.aop.TargetSource getTargetSource()");
    private static final Method GET_TARGET = Method.getMethod("java.lang.Object getTarget()");
    
}
