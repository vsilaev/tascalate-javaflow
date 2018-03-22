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
package org.apache.commons.javaflow.examples.invokedynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

public abstract class AbstractDynamicInvokerGenerator {

    byte[] generateInvokeDynamicRunnable(String dynamicInvokerClassName, 
                                         String dynamicLinkageClassName,
                                         String bootstrapMethodName, 
                                         String targetMethodDescriptor) {

        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, dynamicInvokerClassName, null, 
                 "java/lang/Object", new String[] {"java/lang/Runnable"});
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
            mv.visitAnnotation("Lorg/apache/commons/javaflow/api/continuable;", false);
            mv.visitCode();

            Handle bootstrap = new Handle(H_INVOKESTATIC, 
                                          dynamicLinkageClassName, 
                                          bootstrapMethodName,                   
                                          DYNAMIC_BOOTSTRAP_METHOD_TYPE.toMethodDescriptorString());

            int maxStackSize = addMethodParameters(mv);
            mv.visitInvokeDynamicInsn("runCalculation", targetMethodDescriptor, bootstrap);
            mv.visitInsn(RETURN);
            mv.visitMaxs(maxStackSize, 2);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    protected abstract int addMethodParameters(MethodVisitor mv);

    private static final MethodType DYNAMIC_BOOTSTRAP_METHOD_TYPE = MethodType.methodType(
        CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class
    );

}