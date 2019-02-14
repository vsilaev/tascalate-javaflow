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

import org.apache.commons.javaflow.instrumentation.cdi.owb.OwbProxyClassProcessor;
import org.apache.commons.javaflow.instrumentation.cdi.spring.SpringProxyClassProcessor;
import org.apache.commons.javaflow.instrumentation.cdi.weld.WeldProxyClassProcessor;
import org.apache.commons.javaflow.spi.ContinuableClassInfo;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.StopException;

class CdiProxyClassAdapter extends ExtendedClassVisitor {

    static enum ContainerType {
        WELD("org/jboss/weld/bean/proxy/ProxyObject") {
            
            @Override
            boolean accept(String className, String interfaceName) {
                // *$$_WeldSubclass is not a scope/interceptor proxy
                // Otherwise it's indeed a proxy
                return super.accept(className, interfaceName) && !className.endsWith("$$_WeldSubclass");
            }
            
            @Override
            ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
                return new WeldProxyClassProcessor(api, className, classInfo);
            }            

        }, 
        SPRING("org/springframework/aop/framework/Advised") {
            
            @Override
            ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
                return new SpringProxyClassProcessor(api, className, classInfo);
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
            
        }
        ;

        private Set<String> markerInterfaces;
        
        private ContainerType(String... markerInterfaces) {
            this.markerInterfaces = 
                Collections.unmodifiableSet( new HashSet<String>(Arrays.asList(markerInterfaces)) );
        }
        
        boolean accept(String className, String interfaceName) {
            return markerInterfaces.contains(interfaceName);
        }
        
        abstract ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo);
    }
    
    private final ContinuableClassInfoResolver cciResolver;
    private ProxyClassProcessor processor;

    CdiProxyClassAdapter(ClassVisitor delegate, ContinuableClassInfoResolver cciResolver) {
        super(delegate);
        this.cciResolver = cciResolver;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        ContainerType selectedType = null;
        ContainerType[] cdiEnvironmentTypes = ContainerType.values();
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
        
        ContinuableClassInfo classInfo;
        try {
            classInfo = cciResolver.resolve(superName);
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