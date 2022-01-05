/**
 * ﻿Copyright 2013-2021 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.agent.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.javaflow.providers.asmx.AbstractResourceTransformationFactory;
import org.apache.commons.javaflow.providers.asmx.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.providers.asmx.PartialResourceTransformationFactory;
import org.apache.commons.javaflow.spi.Cache;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformer;

import net.tascalate.asmx.plus.ClassHierarchy;

public class ContinuableProxyTransformationFactory extends AbstractResourceTransformationFactory {

    private final AbstractResourceTransformationFactory helper = new PartialResourceTransformationFactory();
    
    public ResourceTransformer createTransformer(ResourceLoader resourceLoader) {
        SharedState sharedState = CACHED_SHARED.get(resourceLoader);
        return new ContinuableProxyTransformer(
            // Actualize ClassHierarchy per resource loader
            PartialResourceTransformationFactory.shareHierarchy(sharedState.hierarchy, resourceLoader),
            createResolver(resourceLoader),
            sharedState.proxyTypes
        );
    }
    
    public ContinuableClassInfoResolver createResolver(ResourceLoader resourceLoader) {
        return helper.createResolver(resourceLoader);
    }
    
    private static final Cache<ResourceLoader, SharedState> CACHED_SHARED = 
        new Cache<ResourceLoader, SharedState>() {
            @Override
            protected SharedState createValue(ResourceLoader loader) {
                List<ProxyType> proxyTypes = new ArrayList<ProxyType>();
                for (ProxyType proxyType : ProxyType.values()) {
                    if (proxyType.isAvailable(loader)) {
                        proxyTypes.add(proxyType);
                    }
                }
                return new SharedState(PartialResourceTransformationFactory.createHierarchy(loader), proxyTypes);
            }
        }; 
        
    static class SharedState {
        final ClassHierarchy hierarchy;
        final List<ProxyType> proxyTypes;
        
        SharedState(ClassHierarchy hierarchy, List<ProxyType> proxyTypes) {
            this.hierarchy = hierarchy;
            this.proxyTypes = Collections.unmodifiableList(proxyTypes);
        }
    }
}
