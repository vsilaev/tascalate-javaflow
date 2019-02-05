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
package org.apache.commons.javaflow.providers.asm4;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class MaybeContinuableClassVisitor extends ClassVisitor {
    private final Asm4ContinuableClassInfoResolver environment; 
    private boolean classContinuatedMarkerFound = false;
    private String selfclass;
    private String superclass;
    private String[] superinterfaces;
    private String outerClassName;
    private String outerClassMethodName;
    private String outerClassMethodDesc;
    
    private final Map<String, String> actual2accessor = new HashMap<String, String>();
    private final Map<String, String> bridge2specialization = new HashMap<String, String>();
    private final Set<String> desugaredLambdaBodies = new HashSet<String>();
    
    final Set<String> continuableMethods = new HashSet<String>();

    private boolean isAnnotation = false;

    MaybeContinuableClassVisitor(Asm4ContinuableClassInfoResolver environment) {
        super(AsmVersion.CURRENT);
        this.environment = environment;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        isAnnotation = (access & Opcodes.ACC_ANNOTATION) > 0;
        selfclass = name;
        superclass = superName;
        superinterfaces = interfaces;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        if (!isAnnotation && SKIP_ENCHANCING_ANNOTATION.equals(descriptor)) {
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
    public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
        if (isAnnotation) {
            return null;
        }

        boolean isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0;
        boolean isPackagePrivate =  (access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0;
        if (isSynthetic) {
            final boolean isAccessor = isPackagePrivate && name.startsWith("access$") && (access & Opcodes.ACC_STATIC) != 0;
            final boolean isBridge = (access & Opcodes.ACC_BRIDGE) != 0;
            if (isAccessor || isBridge) {
                return new MethodVisitor(this.api) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String targetName, String targetDesc) {
                        if (selfclass.equals(owner)) {
                            if (isAccessor) {
                                actual2accessor.put(targetName + targetDesc, name + desc);
                            }
                            if (isBridge) {
                                bridge2specialization.put(name + desc, targetName + targetDesc);
                            }
                        }
                    }
                };
            }
        }

        // If this method is desugared lambda body
        if ( isSynthetic && isPackagePrivate && name.startsWith("lambda$") ) {
            // RetroLambda desugars method body to package private
            desugaredLambdaBodies.add(name + desc);
            return null;
        }

        return new MethodVisitor(this.api) {

            private boolean methodContinuableAnnotationFound = false;

            @Override
            public AnnotationVisitor visitAnnotation(String description, boolean visible) {
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
        for (Map.Entry<String, String> n2s : actual2accessor.entrySet() ) {
            if (continuableMethods.contains(n2s.getKey())) {
                continuableMethods.add(n2s.getValue());
            }
        }
        visitInheritanceChain();
        checkOuterClass();
        // Check after inheritance -- required by bridged methods (specialization of inherited)
        for (Map.Entry<String, String> n2s : bridge2specialization.entrySet() ) {
            if (continuableMethods.contains(n2s.getKey())) {
                continuableMethods.add(n2s.getValue());
            }
        }
        // Take desugared lambda bodies in consideration always 
        // If there is no calls to continuable inside then
        // there are will be no run-time penalty anyway
        continuableMethods.addAll(desugaredLambdaBodies);
    }

    private boolean inheritanceChainVisited = false;
    private void visitInheritanceChain() {
        if (!inheritanceChainVisited) {
            inheritanceChainVisited = true;
            if (!isAnnotation) {
                if (null != superclass && !OBJECT_CLASS_INTERNAL_NAME.equals(superclass)) 
                    visitParentClass(superclass);
                if (null != superinterfaces) for (String superinterface : superinterfaces)
                    visitParentClass(superinterface);

            }
        }
    }

    private void visitParentClass(String classInternalName) {
        ContinuableClassInfoInternal parent = resolve(classInternalName);
        if (null != parent) {
            continuableMethods.addAll(parent.continuableMethods());
        }
    }

    private void checkOuterClass() {
        if (!isAnnotation && (outerClassName != null && outerClassMethodName != null)) {
            if (!continuableMethods.isEmpty()) {
                ContinuableClassInfoInternal outer = resolve(outerClassName);
                if (null != outer && outer.isContinuableMethod(0, outerClassMethodName, outerClassMethodDesc, null)) {
                    // Reserved;
                }
            }
        }
    }

    private ContinuableClassInfoInternal resolve(String classInternalName) {
        try {
            return (ContinuableClassInfoInternal)environment.resolve(classInternalName);
        } catch (IOException ex) {
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

    static final String SKIP_ENCHANCING_ANNOTATION = "Lorg/apache/commons/javaflow/core/Skip;";

    private 
    static final String OBJECT_CLASS_INTERNAL_NAME = Type.getInternalName(Object.class);
}