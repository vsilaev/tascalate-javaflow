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
package org.apache.commons.javaflow.providers.asm3;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

class MaybeContinuableClassVisitor extends ClassAdapter {
    private final Asm3ContinuableClassInfoResolver environment; 
    private boolean classContinuatedMarkerFound = false;
    private String selfclass;
    private String superclass;
    private String[] superinterfaces;
    private String outerClassName;
    private String outerClassMethodName;
    private String outerClassMethodDesc;
    private Map<String, String> normal2synthetic = new HashMap<String, String>();

    Set<String> continuableMethods = new HashSet<String>();

    private boolean isAnnotation = false;

    public MaybeContinuableClassVisitor(final Asm3ContinuableClassInfoResolver environment) {
        super(NOP);
        this.environment = environment;
    }

    public void visit( int version, int access, String name, String signature, String superName, String[] interfaces ) {
        isAnnotation = (access & Opcodes.ACC_ANNOTATION) > 0;
        selfclass = name;
        superclass = superName;
        superinterfaces = interfaces;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (!isAnnotation && MaybeContinuableClassVisitor.MARKER_FIELD_NAME.equals(name) && (access & Opcodes.ACC_STATIC) != 0) {
            classContinuatedMarkerFound = true;
        }
        return null;
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) { 
        outerClassName = owner;
        outerClassMethodName = name;
        outerClassMethodDesc = desc;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        if (isAnnotation) {
            return null;
        }

        boolean isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0 ;
        if (isSynthetic) {
            boolean isPackagePrivate =  (access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0 ;
            boolean isAccessor = isPackagePrivate && name.startsWith("access$") && (access & Opcodes.ACC_STATIC) != 0;
            boolean isBridge = (access & Opcodes.ACC_BRIDGE) != 0;
            if (isAccessor || isBridge) {
                return new MethodAdapter(NOP) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String targetName, String targetDesc) {
                        if (selfclass.equals(owner)) {
                            normal2synthetic.put(targetName + targetDesc, name + desc);
                        }
                    }
                };
            }
        }
        
        return new MethodAdapter(NOP) {

            private boolean methodContinuableAnnotationFound = false;

            @Override
            public AnnotationVisitor visitAnnotation(final String description, boolean visible) {
                if (!methodContinuableAnnotationFound) {
                    methodContinuableAnnotationFound = environment.isContinuableAnnotation(description);
                }
                return null;
            }

            @Override 
            public void visitEnd() {
                if (methodContinuableAnnotationFound) {
                    continuableMethods.add(name + desc);
                }
            }

        };
    }

    @Override
    public void visitEnd() {
        for (final Map.Entry<String, String> n2s : normal2synthetic.entrySet() ) {
            if (continuableMethods.contains(n2s.getKey())) {
                continuableMethods.add(n2s.getValue());
            }
        }
        visitInheritanceChain();
        checkOuterClass();
    }

    private boolean inheritanceChainVisited = false;
    private void visitInheritanceChain() {
        if (!inheritanceChainVisited) {
            inheritanceChainVisited = true;
            if (!isAnnotation) {
                if (null != superclass && !OBJECT_CLASS_INTERNAL_NAME.equals(superclass)) 
                    visitParentClass(superclass);
                if (null != superinterfaces) for (final String superinterface : superinterfaces)
                    visitParentClass(superinterface);

            }
        }
    }

    private void visitParentClass(final String classInternalName) {
        final ContinuableClassInfoInternal parent = resolve(classInternalName);
        if (null != parent) {
            continuableMethods.addAll(parent.continuableMethods());
        }
    }

    private void checkOuterClass() {
        if (!isAnnotation && (outerClassName != null && outerClassMethodName != null)) {
            if (!continuableMethods.isEmpty()) {
                final ContinuableClassInfoInternal outer = resolve(outerClassName);
                if (null != outer && outer.isContinuableMethod(0, outerClassMethodName, outerClassMethodDesc, null)) {
                    // Reserved;
                }
            }
        }
    }

    private ContinuableClassInfoInternal resolve(final String classInternalName) {
        try {
            return (ContinuableClassInfoInternal)environment.resolve(classInternalName);
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    boolean isContinuable() { 
        return !isAnnotation && !continuableMethods.isEmpty();
    }

    boolean isProcessed() {
        // Processed only after marker field is added
        return classContinuatedMarkerFound;
    }

    final static String MARKER_FIELD_NAME = "___$$$CONT$$$___";

    private final static String OBJECT_CLASS_INTERNAL_NAME = Type.getInternalName(Object.class);

    final static EmptyVisitor NOP = new EmptyVisitor();
}
