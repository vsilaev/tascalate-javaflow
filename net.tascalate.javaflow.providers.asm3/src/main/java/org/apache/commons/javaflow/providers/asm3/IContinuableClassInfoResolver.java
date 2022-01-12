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
package org.apache.commons.javaflow.providers.asm3;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import org.apache.commons.javaflow.spi.ClassMatcher;
import org.apache.commons.javaflow.spi.ResourceLoader;

class IContinuableClassInfoResolver implements ContinuableClassInfoResolver {
    private final Map<String, IContinuableClassInfo> visitedClasses = new HashMap<String, IContinuableClassInfo>();
    private final Set<String> processedAnnotations = new HashSet<String>();
    private final Set<String> continuableAnnotations = new HashSet<String>();
    private final Set<String> refreshClasses = new HashSet<String>();
    private final ResourceLoader resourceLoader;
    private final SharedContinuableClassInfos cciShared;
    
    IContinuableClassInfoResolver(ResourceLoader resourceLoader, SharedContinuableClassInfos cciShared) {
        this.resourceLoader = resourceLoader;
        this.cciShared = cciShared;
    }

    public IContinuableClassInfo resolve(String classInternalName) throws IOException {
        IContinuableClassInfo classInfo = getResolved(classInternalName);
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
    
    public void release() {
        cciShared.mergeWith(visitedClasses, processedAnnotations, continuableAnnotations);
    }
    
    public void reset(Collection<String> classNames) {
        visitedClasses.keySet().removeAll(classNames);
        refreshClasses.addAll(classNames);
    }
    
    IContinuableClassInfo resolve(String classInternalName, byte[] classBytes) {
        IContinuableClassInfo classInfo = getResolved(classInternalName);
        if (classInfo == null) {
            return resolveContinuableClassInfo(classInternalName, new ClassReader(classBytes));
        } else {
            return unmask(classInfo);
        }
    }
    
    ClassMatcher veto() {
        return cciShared.veto();
    }
    
    private IContinuableClassInfo getResolved(String classInternalName) {
        if (refreshClasses.contains(classInternalName)) {
            return null;
        }
        IContinuableClassInfo result;
        
        result = visitedClasses.get(classInternalName);
        if (null != result) {
            return result;
        }
        
        result = cciShared.getResolved(classInternalName);
        if (null != result) {
            return result;
        }
        
        return null;
    }

    private IContinuableClassInfo resolveContinuableClassInfo(String classInternalName, ClassReader reader) {
        MaybeContinuableClassVisitor maybeContinuableClassVisitor = new MaybeContinuableClassVisitor(this); 
        reader.accept(maybeContinuableClassVisitor, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

        IContinuableClassInfo classInfo = maybeContinuableClassVisitor.toContinuableClassInfo();
        visitedClasses.put(classInternalName, null != classInfo ? classInfo : UNSUPPORTED_CLASS_INFO);
        refreshClasses.remove(classInternalName);
        return classInfo;
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
        // Check already resolved shared state first
        if (cciShared.isContinuableAnnotation(annotationClassDescriptor)) {
            return AnnotationProcessingState.SUPPORTED;
        } else if (cciShared.isProcessedAnnotation(annotationClassDescriptor)) {
            return AnnotationProcessingState.UNSUPPORTED;
        }

        // Now check own state
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

    private static IContinuableClassInfo unmask(IContinuableClassInfo classInfo) {
        return classInfo == UNSUPPORTED_CLASS_INFO ? null : classInfo;
    }
    
    private static final IContinuableClassInfo UNSUPPORTED_CLASS_INFO = 
        new IContinuableClassInfo(true, Collections.<String>emptySet());
    
    private static enum AnnotationProcessingState {
        UNKNON, UNSUPPORTED, SUPPORTED;
    }
}