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
package org.apache.commons.javaflow.providers.asm4;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.javaflow.api.ContinuableAnnotation;
import org.apache.commons.javaflow.spi.ContinuableClassInfo;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

class Asm4ContinuableClassInfoResolver implements ContinuableClassInfoResolver {
    final private Map<String, ContinuableClassInfo> visitedClasses = new HashMap<String, ContinuableClassInfo>();
    final private Set<String> processedAnnotations = new HashSet<String>();
    final private Set<String> continuableAnnotations = new HashSet<String>();
    final private ResourceLoader resourceLoader;

    Asm4ContinuableClassInfoResolver(final ResourceLoader classLoader) {
        this.resourceLoader = classLoader;
        markContinuableAnnotation(CONTINUABLE_ANNOTATION_TYPE);
    }

    public ResourceLoader resourceLoader() {
        return resourceLoader;
    }

    public ContinuableClassInfo forget(String className) {
        return visitedClasses.remove(className);
    }

    public ContinuableClassInfo resolve(final String classInternalName, final byte[] classBytes) {
        return resolveContinuableClassInfo(classInternalName, new ClassReader(classBytes));
    }

    public ContinuableClassInfo resolve(final String classInternalName) throws IOException {
        final InputStream classBytes = resourceLoader.getResourceAsStream(classInternalName + ".class");
        try {
            return resolveContinuableClassInfo(classInternalName, new ClassReader(classBytes));
        } finally {
            if (null != classBytes)
                try { classBytes.close(); } catch (final IOException exIgnore) {}
        }
    }

    private ContinuableClassInfo resolveContinuableClassInfo(final String classInternalName, final ClassReader reader) {
        ContinuableClassInfo classInfo = visitedClasses.get(classInternalName);
        if (classInfo == null) {

            final MaybeContinuableClassVisitor maybeContinuableClassVisitor = new MaybeContinuableClassVisitor(this); 
            reader.accept(maybeContinuableClassVisitor, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

            if (maybeContinuableClassVisitor.isContinuable()) {
                classInfo = new ContinuableClassInfoInternal(
                        maybeContinuableClassVisitor.isProcessed(), 
                        maybeContinuableClassVisitor.continuableMethods
                        );
            } else {
                classInfo = UNSUPPORTED_CLASS_INFO;
            }
            visitedClasses.put(classInternalName, classInfo);
        }
        return classInfo == UNSUPPORTED_CLASS_INFO ? null : classInfo;
    }

    private boolean resolveContinuableAnnotation(final String annotationClassDescriptor, final ClassReader reader) {
        final MaybeContinuableAnnotationVisitor maybeContinuableAnnotationVisitor = new MaybeContinuableAnnotationVisitor(this); 
        reader.accept(maybeContinuableAnnotationVisitor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

        if (maybeContinuableAnnotationVisitor.isContinuable()) {
            markContinuableAnnotation(annotationClassDescriptor);
            return true;
        } else {
            return false;
        }
    }

    private AnnotationProcessingState getAnnotationProcessingState(final String annotationClassDescriptor) {
        if (continuableAnnotations.contains(annotationClassDescriptor))
            return AnnotationProcessingState.SUPPORTED;
        else if (processedAnnotations.contains(annotationClassDescriptor))
            return AnnotationProcessingState.UNSUPPORTED;
        else
            return AnnotationProcessingState.UNKNON;
    }

    private void markProcessedAnnotation(final String annotationClassDescriptor) {
        processedAnnotations.add(annotationClassDescriptor);
    }

    private void markContinuableAnnotation(final String annotationClassDescriptor) {
        markProcessedAnnotation(annotationClassDescriptor);
        continuableAnnotations.add(annotationClassDescriptor);
    }

    public boolean isContinuableAnnotation(final String annotationClassDescriptor) {
        switch (getAnnotationProcessingState(annotationClassDescriptor)) {
            case SUPPORTED:
                return true;
            case UNSUPPORTED:
                return false;
            case UNKNON:
                markProcessedAnnotation(annotationClassDescriptor);
    
                final Type type = Type.getType(annotationClassDescriptor);	
                try {
                    final InputStream annotationBytes= resourceLoader.getResourceAsStream(type.getInternalName() + ".class");
                    try {
                        return resolveContinuableAnnotation(annotationClassDescriptor, new ClassReader(annotationBytes));
                    } finally {
                        if (null != annotationBytes) {
                            try { annotationBytes.close(); } catch (final IOException exIgnore) {}
                        }
                    }
                } catch (final IOException ex) {
                    throw new RuntimeException(ex);
                }
            default:
                throw new RuntimeException("Unknown annotation kind");
        }
    }

    final private static String CONTINUABLE_ANNOTATION_TYPE = Type.getDescriptor(ContinuableAnnotation.class);
    final private static ContinuableClassInfo UNSUPPORTED_CLASS_INFO = new ContinuableClassInfo() {

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