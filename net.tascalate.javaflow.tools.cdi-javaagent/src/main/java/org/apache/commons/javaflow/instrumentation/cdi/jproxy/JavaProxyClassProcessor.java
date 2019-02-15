/**
 * ﻿Copyright 2013-2019 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.instrumentation.cdi.jproxy;

import net.tascalate.asmx.MethodVisitor;
import net.tascalate.asmx.Type;
import net.tascalate.asmx.commons.Method;

import org.apache.commons.javaflow.spi.ContinuableClassInfo;

import org.apache.commons.javaflow.instrumentation.cdi.ProxyClassProcessor;
import org.apache.commons.javaflow.instrumentation.cdi.common.ProxiedMethodAdvice;

public class JavaProxyClassProcessor extends ProxyClassProcessor {
    
    public JavaProxyClassProcessor(int api, String className, ContinuableClassInfo classInfo) {
        super(api, className, classInfo);
    }
    
    @Override
    protected MethodVisitor createAdviceAdapter(MethodVisitor mv, int access, String name, String descriptor) {
        return new ProxiedMethodAdvice(api, mv, access, className, name, descriptor) {
            @Override
            protected void loadProxiedInstance() {
                loadThis();
                invokeStatic(JAVA_PROXY_TYPE, GET_INVOCATION_HANDLER);
            }
        };
    }
    
    private static final Type JAVA_PROXY_TYPE = Type.getObjectType("java/lang/reflect/Proxy");
    private static final Method GET_INVOCATION_HANDLER = Method.getMethod("java.lang.reflect.InvocationHandler getInvocationHandler(java.lang.Object)");
}
