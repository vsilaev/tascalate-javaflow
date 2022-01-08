/**
 * ﻿Copyright 2013-2021 Valery Silaev (http://vsilaev.com)
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

import net.tascalate.asmx.FieldVisitor;
import net.tascalate.asmx.MethodVisitor;
import net.tascalate.asmx.Type;

import org.apache.commons.javaflow.providers.core.ContinuableClassInfo;
import org.apache.commons.javaflow.providers.proxy.ExtendedClassVisitor;
import org.apache.commons.javaflow.providers.proxy.ProxyClassProcessor;

public class OwbProxyClassProcessor extends ProxyClassProcessor {
    private Type owbProxiedInstanceType;
    private Type owbProxiedInstanceProviderType;
    
    public OwbProxyClassProcessor(int api, String className, ContinuableClassInfo classInfo) {
        super(api, className, classInfo);
    }
    
    @Override
    protected MethodVisitor createAdviceAdapter(MethodVisitor mv, int acc, String name, String desc) {
        if (null != owbProxiedInstanceType) {
            return new OwbInterceptorProxyMethodAdvice(api, mv, acc, className, name, desc, owbProxiedInstanceType); 
        } else if (null != owbProxiedInstanceProviderType) {
            return new OwbScopeProxyMethodAdvice(api, mv, acc, className, name, desc, owbProxiedInstanceProviderType); 
        } else {
            return mv;
        }
    }  
    
    @Override
    protected FieldVisitor visitField(ExtendedClassVisitor cv, int access, String name, String desc, String signature, Object value) {
        if (OwbInterceptorProxyMethodAdvice.FIELD_PROXIED_INSTANCE.equals(name)) {
            owbProxiedInstanceType = Type.getType(desc);
        }
        if (OwbScopeProxyMethodAdvice.FIELD_INSTANCE_PROVIDER.equals(name)) {
            owbProxiedInstanceProviderType = Type.getType(desc);
        }
        return super.visitField(cv, access, name, desc, signature, value);
    }
}
