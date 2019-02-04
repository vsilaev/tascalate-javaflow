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
package org.apache.commons.javaflow.instrumentation.cdi;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

class AroundOwbInterceptorProxyAdvice extends AroundCdiProxyInvocationAdvice {

    final private Type proxiedInstanceType;

    AroundOwbInterceptorProxyAdvice(int api, MethodVisitor mv, int acc, String className, String methodName, String desc, Type proxiedInstanceType) {
        super(api, mv, acc, className, methodName, desc);
        this.proxiedInstanceType = proxiedInstanceType;
    }

    @Override
    protected void loadProxiedInstance() {
        loadThis();
        getField(Type.getObjectType(className), FIELD_PROXIED_INSTANCE, proxiedInstanceType);
    }

    static final String FIELD_PROXIED_INSTANCE = "owbIntDecProxiedInstance"; //InterceptorDecoratorProxyFactory.FIELD_PROXIED_INSTANCE;

}
