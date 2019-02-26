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
package org.apache.commons.javaflow.instrumentation.cdi.cproxy;

import net.tascalate.asmx.MethodVisitor;
import net.tascalate.asmx.Type;
import net.tascalate.asmx.commons.Method;

import org.apache.commons.javaflow.instrumentation.cdi.ProxyClassProcessor;
import org.apache.commons.javaflow.instrumentation.cdi.common.ProxiedMethodAdvice;
import org.apache.commons.javaflow.providers.asmx.ContinuableClassInfo;

public class CustomProxyClassProcessor extends ProxyClassProcessor {
    
    public CustomProxyClassProcessor(int api, String className, ContinuableClassInfo classInfo) {
        super(api, className, classInfo);
    }
    
    @Override
    protected MethodVisitor createAdviceAdapter(MethodVisitor mv, int access, final String name, final String descriptor) {
        return new ProxiedMethodAdvice(api, mv, access, className, name, descriptor) {
            @Override
            protected void loadProxiedInstance() {
                loadThis();
                push(name);
                push(descriptor);
                invokeInterface(CUSTOM_CONTINUABLE_PROXY_TYPE, GET_INVOCATION_HANDLER);
            }
        };
    }
    
    private static final Type CUSTOM_CONTINUABLE_PROXY_TYPE = Type.getObjectType("org/apache/commons/javaflow/core/CustomContinuableProxy");
    private static final Method GET_INVOCATION_HANDLER = Method.getMethod("java.lang.Object getInvocationHandler(java.lang.String, java.lang.String)");
}
