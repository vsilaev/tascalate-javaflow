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

import java.io.IOException;

import org.apache.commons.javaflow.spi.Cache;
import org.apache.commons.javaflow.spi.ClassMatcher;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformer;
import org.apache.commons.javaflow.spi.VetoableResourceLoader;

import net.tascalate.asmx.plus.ClassHierarchy;

public class PartialResourceTransformationFactory extends AbstractResourceTransformationFactory {

    public ResourceTransformer createTransformer(ResourceLoader resourceLoader) {
        throw new UnsupportedOperationException();
    }
    
    public ContinuableClassInfoResolver createResolver(ResourceLoader resourceLoader) {
        return new IContinuableClassInfoResolver(
            resourceLoader,
            CACHED_SHARED.get(resourceLoader)
        );
    }
    
    @SuppressWarnings("all") //"exports" in Java 9
    public static ClassHierarchy createHierarchy(ResourceLoader resourceLoader) {
        return new ClassHierarchy(new AsmxResourceLoader(resourceLoader));
    }
    
    @SuppressWarnings("all") //"exports" in Java 9
    public static ClassHierarchy shareHierarchy(ClassHierarchy original, ResourceLoader resourceLoader) {
        Object loader = original.loader();
        if (loader instanceof AsmxResourceLoader) {
            AsmxResourceLoader aloader = (AsmxResourceLoader)loader;
            if (aloader.resourceLoader == resourceLoader) {
                return original;
            }
        }
        return original.shareWith(new AsmxResourceLoader(resourceLoader));
    }    

    static SharedContinuableClassInfos getCached(ResourceLoader resourceLoader) {
        return CACHED_SHARED.get(resourceLoader);
    }
    
    static ClassMatcher createVeto(ResourceLoader resourceLoader) {
        if (resourceLoader instanceof VetoableResourceLoader) {
            try {
                return ((VetoableResourceLoader)resourceLoader).createVeto();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return ClassMatcher.MATCH_NONE;
        }
    }
    
    private static final Cache<ResourceLoader, SharedContinuableClassInfos> CACHED_SHARED = 
        new Cache<ResourceLoader, SharedContinuableClassInfos>() {
            @Override
            protected SharedContinuableClassInfos createValue(ResourceLoader loader) {
                return new SharedContinuableClassInfos(
                    createHierarchy(loader), createVeto(loader)
                );
            }
        };    
    
}