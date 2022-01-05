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
package org.apache.commons.javaflow.agent.proxy;

import java.io.IOException;
import java.util.List;

import org.apache.commons.javaflow.providers.asmx.ContinuableClassInfo;
import org.apache.commons.javaflow.providers.asmx.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.StopException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tascalate.asmx.ClassVisitor;
import net.tascalate.asmx.FieldVisitor;
import net.tascalate.asmx.MethodVisitor;
import net.tascalate.asmx.plus.ClassHierarchy;

class ContinuableProxyAdapter extends ExtendedClassVisitor {
    private static final Logger log = LoggerFactory.getLogger(ContinuableProxyAdapter.class);
    
    private final ContinuableClassInfoResolver cciResolver;
    private final ClassHierarchy hierarchy;
    private final List<ProxyType> proxyTypes;
    
    private ProxyClassProcessor processor;

    ContinuableProxyAdapter(ClassVisitor delegate,
                            ClassHierarchy hierarchy,
                            ContinuableClassInfoResolver cciResolver, 
                            List<ProxyType> proxyTypes) {
        super(delegate);
        this.cciResolver = cciResolver;
        this.hierarchy = hierarchy;
        this.proxyTypes = proxyTypes;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        ProxyType selectedType = null;
        for (ProxyType proxyType : proxyTypes) {
            if (proxyType.accept(hierarchy, name, signature, superName, interfaces)) {
                selectedType = proxyType;
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
        if (log.isDebugEnabled()) {
            log.debug("Using " + selectedType + " proxy enhancer for class " + name);
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