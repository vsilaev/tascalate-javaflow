/**
 * ﻿Original work: copyright 1999-2004 The Apache Software Foundation
 * (http://www.apache.org/)
 *
 * This project is based on the work licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Modified work: copyright 2013-2019 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.providers.asm3;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import org.apache.commons.javaflow.spi.ContinuableClassInfo;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.StopException;

/**
 * ContinuableClassVisitor
 * 
 * @author Evgueni Koulechov
 */
class ContinuableClassVisitor extends ClassAdapter {

    private final InheritanceLookup inheritanceLookup;
    private final ContinuableClassInfoResolver cciResolver;
    private final byte[] originalBytes;

    private String className;
    private ContinuableClassInfo classInfo;
    private boolean skipEnchancing = false;

    ContinuableClassVisitor(ClassVisitor cv, 
                            InheritanceLookup inheritanceLookup, 
                            ContinuableClassInfoResolver cciResolver, 
                            byte[] originalBytes) {
        super(cv);
        this.inheritanceLookup = inheritanceLookup;
        this.cciResolver = cciResolver;
        this.originalBytes = originalBytes;
    }

    boolean skipEnchancing() {
        return skipEnchancing;
    }
    
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        classInfo = cciResolver.resolve(name, originalBytes);

        if (null == classInfo || 
            classInfo.isClassProcessed() || 
            cciResolver.veto().matches(name, signature, superName, interfaces)) {
            skipEnchancing = true;
            // Must exit by throwing exception, otherwise NPE is possible in nested visitor
            throw StopException.INSTANCE;
        }
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        if (MaybeContinuableClassVisitor.SKIP_ENCHANCING_ANNOTATION.equals(descriptor)) {
            skipEnchancing = true;
            classInfo.markClassProcessed();
            throw StopException.INSTANCE;
        }       
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitEnd() {
        if (!skipEnchancing) {
            AnnotationVisitor v = super.visitAnnotation(MaybeContinuableClassVisitor.SKIP_ENCHANCING_ANNOTATION, true);
            if (null != v) {
                v.visitEnd();
            }
        }
        super.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        boolean skip = skipEnchancing || null == classInfo || mv == null
                || (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) > 0 || "<init>".equals(name)
                || !classInfo.isContinuableMethod(access, name, desc, signature);
        if (skip) {
            return mv;
        } else {
            return new ContinuableMethodNode(
                access, name, desc, signature, exceptions, 
                className, inheritanceLookup, cciResolver, mv
            );
        }
    }
}
