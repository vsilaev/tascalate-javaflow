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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.javaflow.spi.ContinuableClassInfo;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.StopException;

import net.tascalate.javaflow.asmx.ClassVisitor;
import net.tascalate.javaflow.asmx.FieldVisitor;
import net.tascalate.javaflow.asmx.MethodVisitor;
import net.tascalate.javaflow.asmx.Type;
import net.tascalate.javaflow.asmx.Opcodes;

class CdiProxyClassAdapter extends ClassVisitor {

    static enum CdiEnvironmentType {
        WELD("org/jboss/weld/bean/proxy/ProxyObject") {
            
            @Override
            boolean accept(String className, String interfaceName) {
                // *$$_WeldSubclass is not a scope/interceptor proxy
                // Otherwise it's indeed a proxy
                return super.accept(className, interfaceName) && !className.endsWith("$$_WeldSubclass");
            }
            
            @Override
            MethodVisitor createAdviceAdapter(CdiProxyClassAdapter ca, MethodVisitor mv, int acc, String name, String desc) {
                return new AroundWeldProxyInvocationAdvice(ca.api, mv, acc, ca.className, name, desc);
            }
        }, 
        SPRING("org/springframework/aop/framework/Advised") {
            
            @Override
            MethodVisitor createAdviceAdapter(CdiProxyClassAdapter ca, MethodVisitor mv, int acc, String name, String desc) {
                return new AroundSpringProxyInvocationAdvice(ca.api, mv, acc, ca.className, name, desc);
            }
        }, 
        OWB(
            "org/apache/webbeans/proxy/OwbInterceptorProxy",
            "org/apache/webbeans/proxy/OwbNormalScopeProxy" 
            ) {
            
            @Override
            MethodVisitor createAdviceAdapter(CdiProxyClassAdapter ca, MethodVisitor mv, int acc, String name, String desc) {
                if (null != ca.owbProxiedInstanceType) {
                    return new AroundOwbInterceptorProxyAdvice(ca.api, mv, acc, ca.className, name, desc, ca.owbProxiedInstanceType); 
                } else if (null != ca.owbProxiedInstanceProviderType) {
                    return new AroundOwbScopeProxyAdvice(ca.api, mv, acc, ca.className, name, desc, ca.owbProxiedInstanceProviderType); 
                } else {
                    return mv;
                }
            }            
        }
        ;

        private Set<String> markerInterfaces;
        
        private CdiEnvironmentType(String... markerInterfaces) {
            this.markerInterfaces = 
                Collections.unmodifiableSet( new HashSet<String>(Arrays.asList(markerInterfaces)) );
        }
        
        abstract MethodVisitor createAdviceAdapter(CdiProxyClassAdapter ca, MethodVisitor mv, int acc, String name, String desc);
        boolean accept(String className, String interfaceName) {
            return markerInterfaces.contains(interfaceName);
        }
    }
    
    String className;
    Type owbProxiedInstanceType;
    Type owbProxiedInstanceProviderType;
    
    private CdiEnvironmentType cdiEnvironmentType;
    private ContinuableClassInfo classInfo;

    private final ContinuableClassInfoResolver cciResolver;

    CdiProxyClassAdapter(ClassVisitor delegate, ContinuableClassInfoResolver cciResolver) {
        super(Opcodes.ASM7, delegate);
        this.cciResolver = cciResolver;
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        CdiEnvironmentType selectedType = null;
        CdiEnvironmentType[] cdiEnvironmentTypes = CdiEnvironmentType.values();
        outerLoop:
        for (final String interfaze : interfaces) {
            for (int i = cdiEnvironmentTypes.length - 1; i >= 0; i--) {
                selectedType = cdiEnvironmentTypes[i];
                if (selectedType.accept(name, interfaze)) {
                 // Exclusive, may exit early
                  break outerLoop;  
                } else {
                    selectedType = null;
                }
            }
        }

        if (null == selectedType) {
            throw StopException.INSTANCE;
        }
        
        cdiEnvironmentType = selectedType;

        try {
            classInfo = cciResolver.resolve(superName);
            if (null == classInfo) {
                throw StopException.INSTANCE;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (AroundOwbInterceptorProxyAdvice.FIELD_PROXIED_INSTANCE.equals(name)) {
            owbProxiedInstanceType = Type.getType(desc);
        }
        if (AroundOwbScopeProxyAdvice.FIELD_INSTANCE_PROVIDER.equals(name)) {
            owbProxiedInstanceProviderType = Type.getType(desc);
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int acc, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(acc, name, desc, signature, exceptions);
        if (isContinuableMethodProxy(acc, name, desc, signature, exceptions) && null != cdiEnvironmentType) {
            mv = cdiEnvironmentType.createAdviceAdapter(this, mv, acc, name, desc);
        }
        return mv;
    }

    private boolean isContinuableMethodProxy(int acc, String name, String desc, String signature, String[] exceptions) {
        int idx = name.lastIndexOf("$$super");
        if (idx > 0) {
            name = name.substring(0, idx);
        }
        return ! "<init>".equals(name) && classInfo.isContinuableMethod(acc, name, desc, signature);
    }
}