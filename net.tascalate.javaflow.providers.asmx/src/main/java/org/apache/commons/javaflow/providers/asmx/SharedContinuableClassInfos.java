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
package org.apache.commons.javaflow.providers.asmx;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.javaflow.spi.ClassMatcher;

import net.tascalate.asmx.Type;
import net.tascalate.asmx.plus.ClassHierarchy;

class SharedContinuableClassInfos {
    private final Map<String, IContinuableClassInfo> visitedClasses;
    private final Map<String, Boolean> processedAnnotations;
    private final Map<String, Boolean> continuableAnnotations;

    private final ClassHierarchy hierarhcy;
    private final ClassMatcher veto;
    
    SharedContinuableClassInfos(ClassHierarchy hierarchy, ClassMatcher veto) {
        this.hierarhcy = hierarchy;
        this.veto = veto;
        
        visitedClasses         = new ConcurrentHashMap<String, IContinuableClassInfo>();
        /*
        /* Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>())
         * is not ok for the task -- addAll is not atomic 
         */
        processedAnnotations   = new ConcurrentHashMap<String, Boolean>();
        continuableAnnotations = new ConcurrentHashMap<String, Boolean>();
        
        processedAnnotations.put(CONTINUABLE_ANNOTATION_TYPE.getDescriptor(), Boolean.TRUE);
        continuableAnnotations.put(CONTINUABLE_ANNOTATION_TYPE.getDescriptor(), Boolean.TRUE);
    }
 
    IContinuableClassInfo getResolved(String classInternalName) {
        return visitedClasses.get(classInternalName);
    }
    
    boolean isProcessedAnnotation(String annotationClassDescriptor) {
        return processedAnnotations.containsKey(annotationClassDescriptor);
    }
    
    boolean isContinuableAnnotation(String annotationClassDescriptor) {
        return continuableAnnotations.containsKey(annotationClassDescriptor);
    }
    
    ClassHierarchy hierarchy() {
        return hierarhcy;
    }
    
    ClassMatcher veto() {
        return veto;
    }

    /* synchronized */
    void mergeWith(Map<String, IContinuableClassInfo> newVisitedClasses, 
                   Set<String> newProcessedAnnotations,
                   Set<String> newContinuableAnnotations) {
        
        visitedClasses.putAll(newVisitedClasses);
        continuableAnnotations.putAll(toMap(newContinuableAnnotations));
        processedAnnotations.putAll(toMap(newProcessedAnnotations));
    }
    
    private static Map<String, Boolean> toMap(Set<String> keys) {
        Map<String, Boolean> result = new HashMap<String, Boolean>();
        for (String key : keys) {
            result.put(key, Boolean.TRUE);
        }
        return result;
    }
    
    private static final Type CONTINUABLE_ANNOTATION_TYPE = 
        Type.getObjectType("org/apache/commons/javaflow/api/ContinuableAnnotation");

}
