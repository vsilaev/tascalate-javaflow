/**
 * ﻿Copyright 2013-2017 Valery Silaev (http://vsilaev.com)
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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

abstract class AroundCdiProxyInvocationAdvice extends AdviceAdapter {
    final protected String className;
    final protected String methodName;

    private Label startFinally;
    private int stackRecorderVar;

    protected AroundCdiProxyInvocationAdvice(int api, MethodVisitor mv, int acc, String className, String methodName, String desc) {
        super(api, mv, acc, methodName, desc);
        this.className = className;
        this.methodName = methodName;
    }

    abstract protected void loadProxiedInstance();

    @Override
    protected void onMethodEnter() {
        // verify if restoring
        stackRecorderVar = newLocal(STACK_RECORDER_TYPE);
        Label startDelegated = new Label();

        // PC: StackRecorder stackRecorder = StackRecorder.get();
        stackRecorderGet();
        dup();
        storeLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        // PC: if (stackRecorder != null && stackRecorder.isRestoring) {
        ifNull(startDelegated);
        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        getField(STACK_RECORDER_TYPE, "isRestoring", Type.BOOLEAN_TYPE);
        visitJumpInsn(IFEQ, startDelegated);

        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        stackRecorderPopRef();
        pop();

        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        loadProxiedInstance();
        stackRecorderPushRef();

        visitLabel(startDelegated);

        super.onMethodEnter();
    }

    @Override
    public void visitCode() {
        super.visitCode();
        mv.visitLabel(startFinally = new Label());
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        Label endFinally = new Label();
        mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
        mv.visitLabel(endFinally);
        onFinally(ATHROW);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(maxStack, maxLocals);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != ATHROW) {
            onFinally(opcode);
        }
    }

    private void onFinally(int opcode) {
        Label done = new Label();
        loadLocal(stackRecorderVar);
        // PC: if (stackRecorder != null && stackRecorder.isCapturing) {
        ifNull(done);
        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        getField(STACK_RECORDER_TYPE, "isCapturing", Type.BOOLEAN_TYPE);
        visitJumpInsn(IFEQ, done);

        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        stackRecorderPopRef();
        pop();
        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        loadThis();
        stackRecorderPushRef();
        visitLabel(done);

    }
    
    private void stackRecorderGet() {
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC, 
            STACK_RECORDER_TYPE.getInternalName(), 
            "get", 
            Type.getMethodDescriptor(STACK_RECORDER_TYPE), 
            false
        );
    }
    
    private void stackRecorderPopRef() {
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, 
            STACK_RECORDER_TYPE.getInternalName(), 
            "popReference", 
            Type.getMethodDescriptor(OBJECT_TYPE), 
            false
        );
    }
    
    private void stackRecorderPushRef() {
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, 
            STACK_RECORDER_TYPE.getInternalName(), 
            "pushReference", 
            Type.getMethodDescriptor(Type.VOID_TYPE, OBJECT_TYPE), 
            false
        );
    }

    private static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
    private static final Type STACK_RECORDER_TYPE = Type.getObjectType("org/apache/commons/javaflow/core/StackRecorder");

}
