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
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import org.apache.commons.javaflow.spi.ClassMatcher;
import org.apache.commons.javaflow.spi.ClassMatchers;
import org.apache.commons.javaflow.spi.ContinuableClassInfo;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.VetoableResourceLoader;

class Asm4ContinuableClassInfoResolver implements ContinuableClassInfoResolver {
    private final Map<String, ContinuableClassInfo> visitedClasses = new HashMap<String, ContinuableClassInfo>();
    private final Set<String> processedAnnotations = new HashSet<String>();
    private final Set<String> continuableAnnotations = new HashSet<String>();
    private final ResourceLoader resourceLoader;
    private final ClassMatcher veto;
    
    Asm4ContinuableClassInfoResolver(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.veto = createVeto(resourceLoader);
        markContinuableAnnotation(CONTINUABLE_ANNOTATION_TYPE.getDescriptor());
    }

    public ResourceLoader resourceLoader() {
        return resourceLoader;
    }

    public String readClassName(byte[] classBytes) {
        return new ClassReader(classBytes).getClassName();
    }
    
    public ContinuableClassInfo forget(String className) {
        return visitedClasses.remove(className);
    }

    public ContinuableClassInfo resolve(String classInternalName, byte[] classBytes) {
        ContinuableClassInfo classInfo = visitedClasses.get(classInternalName);
        if (classInfo == null) {
            return resolveContinuableClassInfo(classInternalName, new ClassReader(classBytes));
        } else {
            return unmask(classInfo);
        }
    }

    public ContinuableClassInfo resolve(String classInternalName) throws IOException {
        ContinuableClassInfo classInfo = visitedClasses.get(classInternalName);
        if (classInfo == null) {
            InputStream classBytes = resourceLoader.getResourceAsStream(classInternalName + ".class");
            try {
                return resolveContinuableClassInfo(classInternalName, new ClassReader(classBytes));
            } finally {
                if (null != classBytes) {
                    try { classBytes.close(); } catch (IOException exIgnore) {}
                }
            }
        } else {
            return unmask(classInfo);
        } 
    }
    
    public ClassMatcher veto() {
        return veto;
    }

    private ContinuableClassInfo resolveContinuableClassInfo(String classInternalName, ClassReader reader) {
        MaybeContinuableClassVisitor maybeContinuableClassVisitor = new MaybeContinuableClassVisitor(this); 
        reader.accept(maybeContinuableClassVisitor, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

        ContinuableClassInfo classInfo;
        if (maybeContinuableClassVisitor.isContinuable()) {
            classInfo = new ContinuableClassInfoInternal(
                maybeContinuableClassVisitor.isProcessed(), 
                maybeContinuableClassVisitor.continuableMethods
            );
        } else {
            classInfo = UNSUPPORTED_CLASS_INFO;
        }
        visitedClasses.put(classInternalName, classInfo);
        return unmask(classInfo);
    }

    private boolean resolveContinuableAnnotation(String annotationClassDescriptor, ClassReader reader) {
        MaybeContinuableAnnotationVisitor maybeContinuableAnnotationVisitor = new MaybeContinuableAnnotationVisitor(this); 
        reader.accept(
            maybeContinuableAnnotationVisitor, 
            ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG
        );

        if (maybeContinuableAnnotationVisitor.isContinuable()) {
            markContinuableAnnotation(annotationClassDescriptor);
            return true;
        } else {
            return false;
        }
    }

    private AnnotationProcessingState getAnnotationProcessingState(String annotationClassDescriptor) {
        if (continuableAnnotations.contains(annotationClassDescriptor))
            return AnnotationProcessingState.SUPPORTED;
        else if (processedAnnotations.contains(annotationClassDescriptor))
            return AnnotationProcessingState.UNSUPPORTED;
        else
            return AnnotationProcessingState.UNKNON;
    }

    private void markProcessedAnnotation(String annotationClassDescriptor) {
        processedAnnotations.add(annotationClassDescriptor);
    }

    private void markContinuableAnnotation(String annotationClassDescriptor) {
        markProcessedAnnotation(annotationClassDescriptor);
        continuableAnnotations.add(annotationClassDescriptor);
    }

    public boolean isContinuableAnnotation(String annotationClassDescriptor) {
        switch (getAnnotationProcessingState(annotationClassDescriptor)) {
            case SUPPORTED:
                return true;
            case UNSUPPORTED:
                return false;
            case UNKNON:
                markProcessedAnnotation(annotationClassDescriptor);
    
                final Type type = Type.getType(annotationClassDescriptor);
                try {
                    InputStream annotationBytes= resourceLoader.getResourceAsStream(type.getInternalName() + ".class");
                    try {
                        return resolveContinuableAnnotation(annotationClassDescriptor, new ClassReader(annotationBytes));
                    } finally {
                        if (null != annotationBytes) {
                            try { annotationBytes.close(); } catch (IOException exIgnore) {}
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            default:
                throw new RuntimeException("Unknown annotation kind");
        }
    }
    
    private static ClassMatcher createVeto(ResourceLoader resourceLoader) {
        if (resourceLoader instanceof VetoableResourceLoader) {
            try {
                return ((VetoableResourceLoader)resourceLoader).createVeto();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return ClassMatchers.MATCH_NONE;
        }
    }
    
    private static ContinuableClassInfo unmask(ContinuableClassInfo classInfo) {
        return classInfo == UNSUPPORTED_CLASS_INFO ? null : classInfo;
    }

    private static final Type CONTINUABLE_ANNOTATION_TYPE = Type.getObjectType("org/apache/commons/javaflow/api/ContinuableAnnotation");
    private static final ContinuableClassInfo UNSUPPORTED_CLASS_INFO = new ContinuableClassInfo() {

        public void markClassProcessed() {}

        public boolean isContinuableMethod(int access, String name, String desc, String signature) {
            return false;
        }

        public boolean isClassProcessed() {
            return true;
        }
    };

    private static enum AnnotationProcessingState {
        UNKNON, UNSUPPORTED, SUPPORTED;
    }
}