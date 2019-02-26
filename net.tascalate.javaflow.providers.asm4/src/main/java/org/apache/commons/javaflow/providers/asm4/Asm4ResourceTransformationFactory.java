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

import org.apache.commons.javaflow.spi.Cache;
import org.apache.commons.javaflow.spi.ClassMatcher;
import org.apache.commons.javaflow.spi.ClassMatchers;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;
import org.apache.commons.javaflow.spi.ResourceTransformer;
import org.apache.commons.javaflow.spi.VetoableResourceLoader;

import org.objectweb.asm.ClassReader;

public class Asm4ResourceTransformationFactory implements ResourceTransformationFactory {

    public ResourceTransformer createTransformer(ResourceLoader resourceLoader) {
        SharedContinuableClassInfos cciShared = CACHED_SHARED_CCI.get(resourceLoader);
        return new ContinuableClassTransformer(
            // Actualize ClassHierarchy per resource loader
            cciShared.hierarchy().shareWith(resourceLoader), 
            new IContinuableClassInfoResolver(resourceLoader, cciShared)
        );
    }

    public ContinuableClassInfoResolver createResolver(ResourceLoader resourceLoader) {
        return new IContinuableClassInfoResolver(
            resourceLoader,
            CACHED_SHARED_CCI.get(resourceLoader)
        );
    }
    
    public String readClassName(byte[] classBytes) {
        return new ClassReader(classBytes).getClassName();
    }

    static ClassMatcher createVeto(ResourceLoader resourceLoader) {
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
    
    private static final Cache<ResourceLoader, SharedContinuableClassInfos> CACHED_SHARED_CCI = 
        new Cache<ResourceLoader, SharedContinuableClassInfos>() {
            @Override
            protected SharedContinuableClassInfos createValue(ResourceLoader loader) {
                return new SharedContinuableClassInfos(
                    new ClassHierarchy(loader), createVeto(loader)
                );
            }
        };

}