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
package org.apache.commons.javaflow.providers.asmx;

import java.util.Collection;

import org.apache.commons.javaflow.spi.AbstractResourceTransformer;
import org.apache.commons.javaflow.spi.StopException;

import net.tascalate.asmx.ClassReader;
import net.tascalate.asmx.ClassWriter;

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
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(final String type1, final String type2) {
                return classHierarchy.getCommonSuperClass(type1, type2);
            }
        };
        cciResolver.reset(retransformClasses);
        ContinuableClassVisitor visitor = new ContinuableClassVisitor(
            cw /* BytecodeDebugUtils.decorateClassVisitor(cw, true, * System.err) -- DUMP*/, 
            classHierarchy,
            cciResolver,
            original
        );
        try {
            new ClassReader(original).accept(visitor, ClassReader.SKIP_FRAMES);
        } catch (StopException ex) {
            // Preliminary stop visiting non-continuable class
            return null;
        }

        if (visitor.skipEnchancing()) {
            return null;
        }

        byte[] bytecode = cw.toByteArray();
        // BytecodeDebugUtils.dumpClass(bytecode);
        return bytecode;
    }
    
    public void release() {
        cciResolver.release();
    }
}