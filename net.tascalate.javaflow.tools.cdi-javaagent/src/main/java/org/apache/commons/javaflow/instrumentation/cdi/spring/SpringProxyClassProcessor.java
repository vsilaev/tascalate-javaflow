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
package org.apache.commons.javaflow.instrumentation.cdi.spring;

import net.tascalate.asmx.MethodVisitor;
import net.tascalate.asmx.Type;
import net.tascalate.asmx.commons.Method;

import org.apache.commons.javaflow.instrumentation.cdi.ProxyClassProcessor;
import org.apache.commons.javaflow.instrumentation.cdi.common.ProxiedMethodAdvice;
import org.apache.commons.javaflow.providers.asmx.ContinuableClassInfo;

public class SpringProxyClassProcessor extends ProxyClassProcessor {
    
    public SpringProxyClassProcessor(int api, String className, ContinuableClassInfo classInfo) {
        super(api, className, classInfo);
    }
    
    @Override
    protected MethodVisitor createAdviceAdapter(MethodVisitor mv, int acc, String name, String desc) {
        return new ProxiedMethodAdvice(api, mv, acc, className, name, desc) {
            @Override
            protected void loadProxiedInstance() {
                loadThis();
                invokeVirtual(Type.getObjectType(className), GET_TARGET_SOURCE);
                /*
                 * May be used instead of above
                invokeInterface(Type.getType("org/springframework/aop/framework/Advised"), GET_TARGET_SOURCE);
                */
                invokeInterface(Type.getObjectType("org/springframework/aop/TargetSource"), GET_TARGET);                
            }
        };
    }
    
    private static final Method GET_TARGET_SOURCE = Method.getMethod("org.springframework.aop.TargetSource getTargetSource()");
    private static final Method GET_TARGET = Method.getMethod("java.lang.Object getTarget()");    
}
