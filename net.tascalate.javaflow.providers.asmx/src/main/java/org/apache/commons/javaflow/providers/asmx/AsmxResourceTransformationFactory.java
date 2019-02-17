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
package org.apache.commons.javaflow.providers.asmx;

import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;
import org.apache.commons.javaflow.spi.ResourceTransformer;

public class AsmxResourceTransformationFactory implements ResourceTransformationFactory {

    public ResourceTransformer createTransformer(ContinuableClassInfoResolver cciResolver) {
        return new AsmxClassTransformer(getOrCreateClassHierarchy(cciResolver), cciResolver);
    }

    public ContinuableClassInfoResolver createResolver(ResourceLoader resourceLoader) {
        return new AsmxContinuableClassInfoResolver(resourceLoader);
    }
    
    private static ClassHierarchy getOrCreateClassHierarchy(ContinuableClassInfoResolver cciResolver) {
        ClassHierarchy result;
        synchronized (CACHED_CLASS_HIERARCHY) {
            result = CACHED_CLASS_HIERARCHY.get(cciResolver);
            if (null == result) {
                result = new ClassHierarchy(cciResolver.resourceLoader());
                CACHED_CLASS_HIERARCHY.put(cciResolver, result);
            }
        }
        return result;
    }
    
    private static final Map<ContinuableClassInfoResolver, ClassHierarchy> CACHED_CLASS_HIERARCHY = 
        new WeakHashMap<ContinuableClassInfoResolver, ClassHierarchy>();

}