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
package org.apache.commons.javaflow.providers.asm5;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;

public class FastClassVerifier extends BasicVerifier {

    /**
     * Constructs a new {@link FastClassVerifier} to verify a specific class. This
     * class will not be loaded into the JVM since it may be incorrect.
     *
     */
    public FastClassVerifier()
    {
        super(ASM5);
    }

    @Override
    public BasicValue copyOperation(final AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        // Fix error with analyzer for try-with-resources (it sees uninitialized values)
        if ((insn.getOpcode() == Opcodes.ALOAD || insn.getOpcode() == Opcodes.ASTORE) && !value.isReference()) {
            value = newValue(Type.getType("Lnull;"));
        }
        return super.copyOperation(insn, value);
    }

    @Override
    public BasicValue unaryOperation(final AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        // Fix error with analyzer for try-with-resources (it sees uninitialized values)
        if ((insn.getOpcode() == Opcodes.ARETURN) && !value.isReference()) {
        	value = newValue(Type.getType("Lnull;"));
        }
       	return super.unaryOperation(insn, value);
    }  


    @Override
    public BasicValue newValue(final Type type) {
        if (type == null) {
            return BasicValue.UNINITIALIZED_VALUE;
        }

        final boolean isArray = type.getSort() == Type.ARRAY;
        BasicValue v = super.newValue(type);
        if (BasicValue.REFERENCE_VALUE.equals(v)) {
            if (isArray) {
                v = newValue(type.getElementType());
                String desc = v.getType().getDescriptor();
                for (int i = 0; i < type.getDimensions(); ++i) {
                    desc = '[' + desc;
                }
                v = new BasicValue(Type.getType(desc));
            } else {
                v = new BasicValue(type);
            }
        }
        return v;
    }

    @Override
    protected boolean isSubTypeOf(final BasicValue value, final BasicValue expected) {
        Type expectedType = expected.getType();
        Type type = value.getType();
        switch (expectedType.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                return type.equals(expectedType);
            case Type.ARRAY:
            case Type.OBJECT:
                // We are transforming valid bytecode to (hopefully) valid bytecode
                // hence pairs of "value" and "expected" must be compatible
                return true;//isAssignableFrom(expectedType, type);
            default:
                throw new Error("Internal error");
        }
    }
}