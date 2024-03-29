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
package org.apache.commons.javaflow.providers.proxy.owb;

import org.apache.commons.javaflow.providers.proxy.common.ProxiedMethodAdvice;

import net.tascalate.asmx.MethodVisitor;
import net.tascalate.asmx.Type;
import net.tascalate.asmx.commons.Method;

class OwbScopeProxyMethodAdvice extends ProxiedMethodAdvice {

    final private Type proxiedInstanceProviderType; 

    public OwbScopeProxyMethodAdvice(int api, MethodVisitor mv, int acc, String className, String methodName, String desc, Type proxiedInstanceProviderType) {
        super(api, mv, acc, className, methodName, desc);
        this.proxiedInstanceProviderType = proxiedInstanceProviderType;
    }

    @Override
    protected void loadProxiedInstance() {
        loadThis();
        getField(Type.getObjectType(className), FIELD_INSTANCE_PROVIDER, proxiedInstanceProviderType);
        invokeInterface(PROVIDER, PROVIDER_GET);
    }

    private static final Type PROVIDER = Type.getObjectType("javax/inject/Provider");
    private static final Method PROVIDER_GET = Method.getMethod("java.lang.Object get()");

    static final String FIELD_INSTANCE_PROVIDER = "owbContextualInstanceProvider";//NormalScopeProxyFactory.FIELD_INSTANCE_PROVIDER;
}
