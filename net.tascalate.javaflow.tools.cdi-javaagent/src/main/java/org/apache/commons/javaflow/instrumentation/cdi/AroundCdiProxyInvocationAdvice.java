/**
 * ﻿Copyright 2013-2018 Valery Silaev (http://vsilaev.com)
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

    private Label startFinally;

    protected AroundCdiProxyInvocationAdvice(int api, MethodVisitor mv, int acc, String className, String methodName, String desc) {
        super(api, mv, acc, methodName, desc);
        this.className = className;
    }

    abstract protected void loadProxiedInstance();
    
    @Override
    protected void onMethodEnter() {
        loadProxiedInstance();        
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC, 
            INTERCEPTOR_SUPPORT_TYPE.getInternalName(), 
            "beforeExecution", 
            Type.getMethodDescriptor(Type.VOID_TYPE, OBJECT_TYPE), 
            false
        );
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
        mv.visitMaxs(Math.max(1, maxStack), maxLocals);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != ATHROW) {
            onFinally(opcode);
        }
    }

    private void onFinally(int opcode) {
        loadThis();        
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC, 
            INTERCEPTOR_SUPPORT_TYPE.getInternalName(), 
            "afterExecution", 
            Type.getMethodDescriptor(Type.VOID_TYPE, OBJECT_TYPE), 
            false
        );
    }
    
    private static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
    private static final Type INTERCEPTOR_SUPPORT_TYPE = Type.getObjectType("org/apache/commons/javaflow/api/InterceptorSupport");

}
