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

import net.tascalate.asmx.ClassVisitor;
import net.tascalate.asmx.FieldVisitor;
import net.tascalate.asmx.MethodVisitor;

import org.apache.commons.javaflow.providers.asmx.InheritanceLookup;

import org.apache.commons.javaflow.spi.ContinuableClassInfo;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.StopException;

import org.apache.commons.javaflow.instrumentation.cdi.cglib.CGLibProxyClassProcessor;
import org.apache.commons.javaflow.instrumentation.cdi.cproxy.CustomProxyClassProcessor;
import org.apache.commons.javaflow.instrumentation.cdi.jproxy.JavaProxyClassProcessor;
import org.apache.commons.javaflow.instrumentation.cdi.owb.OwbProxyClassProcessor;
import org.apache.commons.javaflow.instrumentation.cdi.spring.SpringProxyClassProcessor;
import org.apache.commons.javaflow.instrumentation.cdi.weld.WeldProxyClassProcessor;

class CdiProxyClassAdapter extends ExtendedClassVisitor {

    static enum ContainerType {
        WELD("org/jboss/weld/bean/proxy/ProxyObject") {
            
            @Override
            boolean accept(InheritanceLookup lookup, 
                           String className, 
                           String signature, 
                           String superName, 
                           String[] interfaces) {
                // *$$_WeldSubclass is not a scope/interceptor proxy
                // Otherwise it's indeed a proxy
                if (className.endsWith("$$_WeldSubclass")) {
                    return false;
                }
                return super.accept(lookup, className, signature, superName, interfaces);
            }
            
            @Override
            ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
                return new WeldProxyClassProcessor(api, className, classInfo);
            }            

        },
        OWB(
            "org/apache/webbeans/proxy/OwbInterceptorProxy",
            "org/apache/webbeans/proxy/OwbNormalScopeProxy" 
            ) {
            
            @Override
            ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
                return new OwbProxyClassProcessor(api, className, classInfo);
            }
        },
        SPRING("org/springframework/aop/framework/Advised") {
            
            @Override
            ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
                return new SpringProxyClassProcessor(api, className, classInfo);
            }
            
        }, 
        CUSTOM_PROXY("org/apache/commons/javaflow/core/CustomContinuableProxy") {
            
            @Override
            ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
                return new CustomProxyClassProcessor(api, className, classInfo);
            }
        },
        CGLIB("org/apache/commons/javaflow/core/ContinuableProxy") {
            
            @Override
            boolean accept(InheritanceLookup lookup, 
                           String className, 
                           String signature, 
                           String superName, 
                           String[] interfaces) {
                // Check that this is CGLib proxy
                // Otherwise it's a regular Java proxy
                String cglibProxyBase = "net/sf/cglib/proxy/Proxy$ProxyImpl";
                if (cglibProxyBase.equals(superName) ||
                    (lookup.isClassAvailable(cglibProxyBase) && lookup.isSubClass(className, cglibProxyBase))) {
                    return super.accept(lookup, className, signature, superName, interfaces);
                } else {
                    return false;
                }
            }
            
            @Override
            ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
                return new CGLibProxyClassProcessor(api, className, classInfo);
            }
        },
        JAVA("org/apache/commons/javaflow/core/ContinuableProxy" /* SAME AS CGLIB, SHOULD FOLLOW */) {
            
            @Override
            ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
                return new JavaProxyClassProcessor(api, className, classInfo);
            }            
        }
        ;

        private Set<String> markerInterfaces;
        
        private ContainerType(String... markerInterfaces) {
            this.markerInterfaces = 
                Collections.unmodifiableSet( new HashSet<String>(Arrays.asList(markerInterfaces)) );
        }
        
        boolean accept(InheritanceLookup lookup, 
                       String className, 
                       String signature, 
                       String superName, 
                       String[] interfaces) {
            // Fast route
            for (String intf : interfaces) {
                if (markerInterfaces.contains(intf)) {
                    return true;
                }
            }
            // Safe route
            for (String intf : markerInterfaces) {
                if (!lookup.isClassAvailable(intf)) {
                    continue;
                }
                if (lookup.isSubClass(className, intf)) {
                    return true;
                }
            }
            return false;
        }
        
        abstract ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo);
    }
    
    private final ContinuableClassInfoResolver cciResolver;
    private final InheritanceLookup lookup;
    private ProxyClassProcessor processor;

    CdiProxyClassAdapter(ClassVisitor delegate, ContinuableClassInfoResolver cciResolver, InheritanceLookup lookup) {
        super(delegate);
        this.cciResolver = cciResolver;
        this.lookup = lookup;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        ContainerType selectedType = null;
        for (ContainerType containerType : ContainerType.values()) {
            if (containerType.accept(lookup, name, signature, superName, interfaces)) {
                selectedType = containerType;
                break;
            }
        }

        if (null == selectedType) {
            throw StopException.INSTANCE;
        }
        
        ContinuableClassInfo classInfo;
        try {
            /**
            Works for CDI but not for libs like CGLIB or java.lang.Proxy 
            -- where implementation is inside handler
            classInfo = cciResolver.resolve(superName);
             */
            classInfo = cciResolver.resolve(name);
            if (null == classInfo) {
                throw StopException.INSTANCE;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        processor = selectedType.createProcessor(api, name, classInfo);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return processor.visitField(this, access, name, descriptor, signature, value);
    }
    
    @Override
    public void visitEnd() {
        processor.visitEnd(this);
    }
    
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return processor.visitMethod(this, access, name, descriptor, signature, exceptions);
    }
}