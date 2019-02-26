/**
 * ﻿Copyright 2013-2019 Valery Silaev (http://vsilaev.com)
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

import org.apache.commons.javaflow.providers.asmx.ContinuableClassInfo;

import net.tascalate.asmx.FieldVisitor;
import net.tascalate.asmx.MethodVisitor;

abstract public class ProxyClassProcessor {
    protected final int api;
    protected final String className;
    protected final ContinuableClassInfo classInfo;
    
    protected ProxyClassProcessor(int api, String className, ContinuableClassInfo classInfo) {
        this.api = api;
        this.className = className;
        this.classInfo = classInfo;
    }
    
    protected MethodVisitor visitMethod(ExtendedClassVisitor cv, int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = cv.defaultVisitMethod(access, name, descriptor, signature, exceptions);
        if (isContinuableMethodProxy(access, name, descriptor, signature, exceptions)) {
            mv = createAdviceAdapter(mv, access, name, descriptor);
        }
        return mv;
    }
    
    protected FieldVisitor visitField(ExtendedClassVisitor cv, int access, String name, String descriptor, String signature, Object value) {
        return cv.defaultVisitField(access, name, descriptor, signature, value);
    }
    
    protected void visitEnd(ExtendedClassVisitor cv) {
        cv.defaultVisitEnd();
    }
    
    abstract protected MethodVisitor createAdviceAdapter(MethodVisitor mv, int access, String name, String descriptor);
    
    protected boolean isContinuableMethodProxy(int access, String name, String descriptor, String signature, String[] exceptions) {
        int idx = name.lastIndexOf("$$super");
        if (idx > 0) {
            name = name.substring(0, idx);
        }
        return ! "<init>".equals(name) && classInfo.isContinuableMethod(access, name, descriptor, signature);
    }
}
