/**
 * ﻿Original work: copyright 1999-2004 The Apache Software Foundation
 * (http://www.apache.org/)
 *
 * This project is based on the work licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Modified work: copyright 2013-2022 Valery Silaev (http://vsilaev.com)
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

import java.util.Collection;

import org.apache.commons.javaflow.spi.AbstractResourceTransformer;
import org.apache.commons.javaflow.spi.StopException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
/**
 * AsmClassTransformer
 * 
 * @author Eugene Kuleshov
 */
class ContinuableClassTransformer extends AbstractResourceTransformer {

    private final ClassHierarchy classHierarchy;
    private final IContinuableClassInfoResolver cciResolver;

    ContinuableClassTransformer(ClassHierarchy classHierarchy, IContinuableClassInfoResolver cciResolver) {
        this.classHierarchy = classHierarchy;
        this.cciResolver = cciResolver;
    }

    public byte[] transform(byte[] original, Collection<String> retransformClasses) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new OfflineClassWriter(classHierarchy, reader, ClassWriter.COMPUTE_FRAMES);
        ContinuableClassVisitor visitor = new ContinuableClassVisitor(
            writer, /* BytecodeDebugUtils.decorateClassVisitor(cw, true, * System.err) -- DUMP*/ 
            classHierarchy,
            cciResolver,
            original
        );
        cciResolver.reset(retransformClasses);
        try {
            reader.accept(visitor, ClassReader.SKIP_FRAMES);
        } catch (StopException ex) {
            // Preliminary stop visiting non-continuable class
            return null;
        }

        if (visitor.skipEnchancing()) {
            return null;
        }

        byte[] bytecode = writer.toByteArray();
        // BytecodeDebugUtils.dumpClass(bytecode);
        return bytecode;
    }
    
    public void release() {
        cciResolver.release();
    }
}