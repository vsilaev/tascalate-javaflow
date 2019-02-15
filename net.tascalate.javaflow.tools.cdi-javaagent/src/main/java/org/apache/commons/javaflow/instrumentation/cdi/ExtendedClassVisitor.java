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

import net.tascalate.asmx.ClassVisitor;
import net.tascalate.asmx.FieldVisitor;
import net.tascalate.asmx.MethodVisitor;
import net.tascalate.asmx.Opcodes;

public abstract class ExtendedClassVisitor extends ClassVisitor {
    
    ExtendedClassVisitor(ClassVisitor delegate) {
        super(Opcodes.ASM7, delegate);
    }
    
    @SuppressWarnings("exports")
	public final FieldVisitor defaultVisitField(int access, String name, String descriptor, String signature, Object value) {
        return super.visitField(access, name, descriptor, signature, value);
    }
    
    @SuppressWarnings("exports")
	public final MethodVisitor defaultVisitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
    
    public final void defaultVisitEnd() {
        super.visitEnd();
    }
}
