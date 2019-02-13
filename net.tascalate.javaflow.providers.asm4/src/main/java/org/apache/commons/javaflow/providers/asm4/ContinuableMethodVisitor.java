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
package org.apache.commons.javaflow.providers.asm4;

import static org.objectweb.asm.Opcodes.*;

import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

class ContinuableMethodVisitor extends MethodVisitor {
    private static final String STACK_RECORDER = "org/apache/commons/javaflow/core/StackRecorder";
    private static final String POP_METHOD = "pop";
    private static final String PUSH_METHOD = "push";

    private final ContinuableMethodNode methodNode;
    private final Label startLabel = new Label();
    private final List<Label> labels;
    private final List<AbstractInsnNode> nodes;
    private final int stackRecorderVar;

    private int currentIndex = 0;
    private Frame currentFrame = null;


    ContinuableMethodVisitor(ContinuableMethodNode a) {
        super(AsmVersion.CURRENT, a.mv);
        this.methodNode = a;
        this.labels = a.labels;
        this.nodes = a.nodes;
        this.stackRecorderVar = a.stackRecorderVar;
    }

    private static Type[] getArgumentTypes(AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            MethodInsnNode mnode = (MethodInsnNode)node;
            return Type.getArgumentTypes(mnode.desc);
        } else {
            InvokeDynamicInsnNode mnode = (InvokeDynamicInsnNode)node; 
            return Type.getArgumentTypes(mnode.desc);
        }
    }

    private static int getOwnerSize(AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            return node.getOpcode() == INVOKESTATIC ? 0 : 1;
        } else {
            // INVOKEDYNAMIC
            return 0;
        }
    }

    @Override
    public void visitCode() {
        mv.visitCode();

        int fsize = labels.size();
        Label[] restoreLabels = new Label[fsize];
        for (int i = 0; i < restoreLabels.length; i++) {
            restoreLabels[i] = new Label();
        }
        // verify if restoring
        Label l0 = new Label();

        // PC: StackRecorder stackRecorder = StackRecorder.get();
        mv.visitMethodInsn(INVOKESTATIC, STACK_RECORDER, "get", "()L" + STACK_RECORDER + ";");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ASTORE, stackRecorderVar);
        mv.visitLabel(startLabel);

        // PC: if (stackRecorder != null && !stackRecorder.isRestoring) {  
        mv.visitJumpInsn(IFNULL, l0);
        mv.visitVarInsn(ALOAD, stackRecorderVar);
        mv.visitFieldInsn(GETFIELD, STACK_RECORDER, "isRestoring", "Z");
        mv.visitJumpInsn(IFEQ, l0);

        mv.visitVarInsn(ALOAD, stackRecorderVar);
        // PC:    stackRecorder.popInt();
        mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, "popInt", "()I");
        mv.visitTableSwitchInsn(0, fsize - 1, l0, restoreLabels);

        // switch cases
        for (int i = 0; i < fsize; i++) {
            Label frameLabel = (Label) labels.get(i);
            mv.visitLabel(restoreLabels[i]);

            AbstractInsnNode mnode = (AbstractInsnNode) nodes.get(i);
            //Frame frame = analyzer.getFrames()[methodNode.getIndex(mnode)];
            Frame frame = methodNode.getFrameByNode(mnode);

            // for each local variable store the value in locals popping it from the stack!
            // locals
            int lsize = frame.getLocals();
            for (int j = lsize - 1; j >= 0; j--) {
                BasicValue value = (BasicValue) frame.getLocal(j);
                if (isNull(value)) {
                    mv.visitInsn(ACONST_NULL);
                    mv.visitVarInsn(ASTORE, j);
                } else if (value == BasicValue.UNINITIALIZED_VALUE) {
                    // TODO ??
                } else if (value == BasicValue.RETURNADDRESS_VALUE) {
                    // TODO ??
                } else {
                    mv.visitVarInsn(ALOAD, stackRecorderVar);
                    Type type = value.getType();
                    if (value.isReference()) {
                        mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, POP_METHOD + "Object", "()Ljava/lang/Object;");
                        Type t = value.getType();
                        String desc = t.getDescriptor();
                        if (desc.charAt(0) == '[') {
                            mv.visitTypeInsn(CHECKCAST, desc);
                        } else {
                            mv.visitTypeInsn(CHECKCAST, t.getInternalName());
                        }
                        mv.visitVarInsn(ASTORE, j);

                    } else {
                        mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, getPopMethod(type), "()" + type.getDescriptor());
                        mv.visitVarInsn(type.getOpcode(ISTORE), j);
                    }
                }
            }

            if (frame instanceof MonitoringFrame) {
                int[] monitoredLocals = ((MonitoringFrame) frame).getMonitored();
                //System.out.println(System.identityHashCode(frame)+" AMonitored locals "+monitoredLocals.length);
                for (int j = 0; j < monitoredLocals.length; j++) {
                    //System.out.println(System.identityHashCode(frame)+" AMonitored local "+monitoredLocals[j]);
                    mv.visitVarInsn(ALOAD, monitoredLocals[j]);
                    mv.visitInsn(MONITORENTER);
                }
            }

            // stack
            Type[] paramTypes = getArgumentTypes(mnode);
            int argSize = paramTypes.length;
            int ownerSize = getOwnerSize(mnode);
            int initSize = mnode.getOpcode() == INVOKESPECIAL && MethodInsnNode.class.cast(mnode).name.equals("<init>") ? 2 : 0;
            int ssize = frame.getStackSize();

            for (int j = 0; j < ssize - argSize - ownerSize - initSize; j++) {
                BasicValue value = (BasicValue) frame.getStack(j);
                if (isNull(value)) {
                    mv.visitInsn(ACONST_NULL);
                } else if (value == BasicValue.UNINITIALIZED_VALUE) {
                    // TODO ??
                } else if (value == BasicValue.RETURNADDRESS_VALUE) {
                    // TODO ??
                } else if (value.isReference()) {
                    mv.visitVarInsn(ALOAD, stackRecorderVar);
                    mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, POP_METHOD + "Object", "()Ljava/lang/Object;");
                    mv.visitTypeInsn(CHECKCAST, value.getType().getInternalName());
                } else {
                    Type type = value.getType();
                    mv.visitVarInsn(ALOAD, stackRecorderVar);
                    mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, getPopMethod(type), "()" + type.getDescriptor());
                }
            }

            if (ownerSize > 0) {
                // Load the object whose method we are calling  
                BasicValue value = ((BasicValue) frame.getStack(ssize - argSize - 1));
                if (isNull(value)) { 
                    // If user code causes NPE, then we keep this behavior: load null to get NPE at runtime 
                    mv.visitInsn(ACONST_NULL);
                } else {
                    mv.visitVarInsn(ALOAD, stackRecorderVar);
                    mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, POP_METHOD + "Reference", "()Ljava/lang/Object;");
                    mv.visitTypeInsn(CHECKCAST, value.getType().getInternalName());
                }
            }

            // Create null types for the parameters of the method invocation

            for (int j = 0; j < argSize; j++) {
                pushDefault(paramTypes[j]);
            }

            // continue to the next method
            mv.visitJumpInsn(GOTO, frameLabel);
        }

        // PC: }
        // end of start block
        mv.visitLabel(l0);
    }

    @Override
    public void visitLabel(Label label) {
        if (currentIndex < labels.size() && label == labels.get(currentIndex)) {
            //int i = methodNode.getIndex((AbstractInsnNode)nodes.get(currentIndex));
            //currentFrame = analyzer.getFrames()[i];
            currentFrame = methodNode.getFrameByNode(nodes.get(currentIndex));
        }
        mv.visitLabel(label);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        visitCall(Opcodes.INVOKEDYNAMIC, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        mv.visitMethodInsn(opcode, owner, name, desc);
        visitCall(opcode, desc);
    }

    private void visitCall(int opcode, String desc) {
        if (currentFrame != null) {
            Label fl = new Label();

            mv.visitVarInsn(ALOAD, stackRecorderVar);
            mv.visitJumpInsn(IFNULL, fl);
            mv.visitVarInsn(ALOAD, stackRecorderVar);
            mv.visitFieldInsn(GETFIELD, STACK_RECORDER, "isCapturing", "Z");
            mv.visitJumpInsn(IFEQ, fl);

            // save stack
            Type returnType = Type.getReturnType(desc);
            boolean hasReturn = returnType != Type.VOID_TYPE;
            if (hasReturn) {
                mv.visitInsn(returnType.getSize() == 1 ? POP : POP2);
            }

            Type[] params = Type.getArgumentTypes(desc);
            int argSize = params.length;
            int ownerSize = opcode == INVOKESTATIC || opcode == INVOKEDYNAMIC ? 0 : 1;  // TODO
            int ssize = currentFrame.getStackSize() - argSize - ownerSize;
            for (int i = ssize - 1; i >= 0; i--) {
                BasicValue value = (BasicValue) currentFrame.getStack(i);
                if (isNull(value)) {
                    mv.visitInsn(POP);
                } else if (value == BasicValue.UNINITIALIZED_VALUE) {
                    // TODO ??
                } else if (value.isReference()) {
                    mv.visitVarInsn(ALOAD, stackRecorderVar);
                    mv.visitInsn(SWAP);
                    mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, PUSH_METHOD + "Object", "(Ljava/lang/Object;)V");
                } else {
                    Type type = value.getType();
                    if (type.getSize() > 1) {
                        mv.visitInsn(ACONST_NULL); // dummy stack entry
                        mv.visitVarInsn(ALOAD, stackRecorderVar);
                        mv.visitInsn(DUP2_X2);  // swap2 for long/double
                        mv.visitInsn(POP2);
                        mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, getPushMethod(type), "(" + type.getDescriptor() + ")V");
                        mv.visitInsn(POP);  // remove dummy stack entry
                    } else {
                        mv.visitVarInsn(ALOAD, stackRecorderVar);
                        mv.visitInsn(SWAP);
                        mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, getPushMethod(type), "(" + type.getDescriptor() + ")V");
                    }
                }
            }

            boolean isInstanceMethod = (methodNode.access & ACC_STATIC) == 0;
            if (isInstanceMethod) {
                mv.visitVarInsn(ALOAD, stackRecorderVar);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, PUSH_METHOD + "Reference", "(Ljava/lang/Object;)V");
            }

            // save locals
            int fsize = currentFrame.getLocals();
            for (int j = 0; j < fsize; j++) {
                BasicValue value = (BasicValue) currentFrame.getLocal(j);
                if (isNull(value)) {
                    // no need to save null
                } else if (value == BasicValue.UNINITIALIZED_VALUE) {
                    // no need to save uninitialized objects
                } else if (value.isReference()) {
                    mv.visitVarInsn(ALOAD, stackRecorderVar);
                    mv.visitVarInsn(ALOAD, j);
                    mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, PUSH_METHOD + "Object", "(Ljava/lang/Object;)V");
                } else {
                    mv.visitVarInsn(ALOAD, stackRecorderVar);
                    Type type = value.getType();
                    mv.visitVarInsn(type.getOpcode(ILOAD), j);
                    mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, getPushMethod(type), "(" + type.getDescriptor() + ")V");
                }
            }

            mv.visitVarInsn(ALOAD, stackRecorderVar);
            if (currentIndex <= 5)
                mv.visitInsn(ICONST_0 + currentIndex);
            else
                mv.visitIntInsn(SIPUSH, currentIndex);
            mv.visitMethodInsn(INVOKEVIRTUAL, STACK_RECORDER, "pushInt", "(I)V");

            if (currentFrame instanceof MonitoringFrame) {
                int[] monitoredLocals = ((MonitoringFrame) currentFrame).getMonitored();
                //System.out.println(System.identityHashCode(currentFrame)+" Monitored locals "+monitoredLocals.length);
                for (int j = 0; j < monitoredLocals.length; j++) {
                    //System.out.println(System.identityHashCode(currentFrame)+" Monitored local "+monitoredLocals[j]);
                    mv.visitVarInsn(ALOAD, monitoredLocals[j]);
                    mv.visitInsn(MONITOREXIT);
                }
            }

            Type methodReturnType = Type.getReturnType(methodNode.desc);
            pushDefault(methodReturnType);
            mv.visitInsn(methodReturnType.getOpcode(IRETURN));
            mv.visitLabel(fl);

            currentIndex++;
            currentFrame = null;
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        Label endLabel = new Label();
        mv.visitLabel(endLabel);

        mv.visitLocalVariable("__stackRecorder", "L" + STACK_RECORDER + ";", null, startLabel, endLabel, stackRecorderVar);

        mv.visitMaxs(maxStack, maxLocals + 1);
        //was mv.visitMaxs(0, 0);
    }

    private static boolean isNull(BasicValue value) {
        if (null == value)
            return true;
        if (!value.isReference())
            return false;
        Type type = value.getType();
        return "Lnull;".equals(type.getDescriptor()); 
    }

    private void pushDefault(Type type) {
        switch (type.getSort()) {
            case Type.VOID:
                break;
            case Type.DOUBLE:
                mv.visitInsn(DCONST_0);
                break;
            case Type.LONG:
                mv.visitInsn(LCONST_0);
                break;
            case Type.FLOAT:
                mv.visitInsn(FCONST_0);
                break;
            case Type.OBJECT:
            case Type.ARRAY:
                mv.visitInsn(ACONST_NULL);
                break;
            default:
                mv.visitInsn(ICONST_0);
                break;
        }
    }

    private static String[] SUFFIXES = {
            "Object",  // 0 void
            "Int",     // 1 boolean
            "Int",     // 2 char
            "Int",     // 3 byte
            "Int",     // 4 short
            "Int",     // 5 int
            "Float",   // 6 float
            "Long",    // 7 long
            "Double",  // 8 double
            "Object",  // 9 array
            "Object",  // 10 object
    };


    private static String getPopMethod(Type type) {
        return POP_METHOD + SUFFIXES[type.getSort()];
    }

    private static String getPushMethod(Type type) {
        return PUSH_METHOD + SUFFIXES[type.getSort()];
    }
}