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

import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;
import org.apache.commons.javaflow.spi.ResourceTransformer;

public class Asm3ResourceTransformationFactory implements ResourceTransformationFactory {

    public ResourceTransformer createTransformer(ContinuableClassInfoResolver cciResolver) {
        return new Asm3ClassTransformer(getOrCreateInheritanceLookup(cciResolver), cciResolver);
    }

    public ContinuableClassInfoResolver createResolver(ResourceLoader resourceLoader) {
        return new Asm3ContinuableClassInfoResolver(resourceLoader);
    }
    
    private static InheritanceLookup getOrCreateInheritanceLookup(ContinuableClassInfoResolver cciResolver) {
        InheritanceLookup result;
        synchronized (CACHED_INHERITANCE_LOOKUP) {
            result = CACHED_INHERITANCE_LOOKUP.get(cciResolver);
            if (null == result) {
                result = new InheritanceLookup(cciResolver.resourceLoader());
                CACHED_INHERITANCE_LOOKUP.put(cciResolver, result);
            }
        }
        return result;
    }
    
    private static final Map<ContinuableClassInfoResolver, InheritanceLookup> CACHED_INHERITANCE_LOOKUP = 
        new WeakHashMap<ContinuableClassInfoResolver, InheritanceLookup>();

}