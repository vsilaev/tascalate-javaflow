/**
 * ﻿Original work: copyright 1999-2004 The Apache Software Foundation
 * (http://www.apache.org/)
 *
 * This project is based on the work licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Modified work: copyright 2013-2017 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.providers.asm4;

import org.apache.commons.javaflow.spi.ContinuableClassInfo;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.StopException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ContinuableClassVisitor
 * 
 * @author Evgueni Koulechov
 */
public class ContinuableClassVisitor extends ClassVisitor {

    final private ContinuableClassInfoResolver cciResolver;
    final private byte[] originalBytes;

    private String className;
    private ContinuableClassInfo classInfo;
    private boolean skipEnchancing = false;
    private boolean isInterface = false;

    public ContinuableClassVisitor(final ClassVisitor cv, final ContinuableClassInfoResolver cciResolver, final byte[] originalBytes) {
        super(Opcodes.ASM4, cv);
        this.cciResolver = cciResolver;
        this.originalBytes = originalBytes;
    }

    boolean skipEnchancing() {
        return skipEnchancing;
    }
    
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        
        className = name;
        classInfo = cciResolver.resolve(name, originalBytes);

        if (null == classInfo || classInfo.isClassProcessed() || StopException.__dirtyCheckSkipContinuationsOnClass(version, access, name, signature, superName, interfaces)) {
            skipEnchancing = true;
            // Must exit by throwing exception, otherwise NPE is possible in nested visitor
            throw StopException.INSTANCE;
        }
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (MaybeContinuableClassVisitor.MARKER_FIELD_NAME.equals(name) && (access & Opcodes.ACC_STATIC) != 0) {
            skipEnchancing = true;
            classInfo.markClassProcessed();
            throw StopException.INSTANCE;
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitEnd() {
        if (!skipEnchancing) {
            super.visitField(
                    (isInterface ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE) + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC, 
                    MaybeContinuableClassVisitor.MARKER_FIELD_NAME, 
                    "Ljava/lang/String;", 
                    null, 
                    "A"
                    )
            .visitEnd();
        }
        super.visitEnd();
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        final boolean skip = skipEnchancing || null == classInfo || mv == null
                || (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) > 0 || "<init>".equals(name)
                || !classInfo.isContinuableMethod(access, name, desc, signature);
        if (skip) {
            return mv;
        } else {
            return new ContinuableMethodNode(access, name, desc, signature, exceptions, className, cciResolver, mv);
        }
    }
}
