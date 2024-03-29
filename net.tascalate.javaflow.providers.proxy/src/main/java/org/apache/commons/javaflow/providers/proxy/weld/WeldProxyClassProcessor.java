/**
 * ﻿Copyright 2013-2022 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.providers.proxy.weld;

import net.tascalate.asmx.MethodVisitor;
import net.tascalate.asmx.Opcodes;
import net.tascalate.asmx.Type;
import net.tascalate.asmx.commons.Method;

import org.apache.commons.javaflow.providers.core.ContinuableClassInfo;
import org.apache.commons.javaflow.providers.proxy.ExtendedClassVisitor;
import org.apache.commons.javaflow.providers.proxy.ProxyClassProcessor;
import org.apache.commons.javaflow.providers.proxy.common.ProxiedMethodAdvice;

public class WeldProxyClassProcessor extends ProxyClassProcessor {
    
    boolean hasTargetInstanceMethod = false;
    
    public WeldProxyClassProcessor(int api, String className, ContinuableClassInfo classInfo) {
        super(api, className, classInfo);
    }
    
    @Override
    protected MethodVisitor visitMethod(ExtendedClassVisitor cv, int access, String name, String descriptor, String signature, String[] exceptions) {
        Method m = GET_TARGET_INSTANCE; 
        // True if NEW weld_getTargetInstance() is present
        hasTargetInstanceMethod |= m.getName().equals(name) && m.getDescriptor().equals(descriptor);
        return super.visitMethod(cv, access, name, descriptor, signature, exceptions);
    }
    
    @Override
    protected MethodVisitor createAdviceAdapter(MethodVisitor mv, int access, String name, String descriptor) {
        return new ProxiedMethodAdvice(api, mv, access, className, name, descriptor) {
            @Override
            protected void loadProxiedInstance() {
                loadThis();
                invokeVirtual(Type.getObjectType(className), GET_TARGET_INSTANCE);
            }
        };
    }
    
    @Override
    protected void visitEnd(ExtendedClassVisitor cv) {
        if (!hasTargetInstanceMethod) {
            // If NEW weld_getTargetInstance() is missing
            // then generate it and delegate call to OLD getTargetInstance()
            Method m = GET_TARGET_INSTANCE; 
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_SYNTHETIC + Opcodes.ACC_FINAL, m.getName(), m.getDescriptor(), null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
                               className, 
                               "getTargetInstance", 
                               m.getDescriptor(), 
                               false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        super.visitEnd(cv);
    }
    
    private static final Method GET_TARGET_INSTANCE = Method.getMethod("java.lang.Object weld_getTargetInstance()");
}
