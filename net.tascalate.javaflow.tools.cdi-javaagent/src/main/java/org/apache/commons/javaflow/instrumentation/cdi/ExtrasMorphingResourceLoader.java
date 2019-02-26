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
package org.apache.commons.javaflow.instrumentation.cdi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.javaflow.spi.MorphingResourceLoader;
import org.apache.commons.javaflow.spi.ResourceLoader;

class ExtrasMorphingResourceLoader extends MorphingResourceLoader {

    private List<ProxyType> proxyTypes;
    
    ExtrasMorphingResourceLoader(ResourceLoader delegate) {
        super(delegate);
    }
    
    ExtrasMorphingResourceLoader(ResourceLoader delegate, Map<String, byte[]> extraResources) {
        super(delegate, extraResources);
    }
    
    private ExtrasMorphingResourceLoader(ResourceLoader delegate, Map<String, byte[]> extraResources, List<ProxyType> proxyTypes) {
        super(delegate, extraResources);
        this.proxyTypes = proxyTypes;
    }

    @Override
    public MorphingResourceLoader withReplacement(Map<String, byte[]> alternativeResources) {
        return new ExtrasMorphingResourceLoader(delegate, alternativeResources, proxyTypes);
    }
    
    @Override
    public MorphingResourceLoader withAddition(Map<String, byte[]> additionalResources) {
        return new ExtrasMorphingResourceLoader(this, additionalResources, proxyTypes);
    }
    
    void init() {
        proxyTypes = new ArrayList<ProxyType>();
        for (ProxyType proxyType : ProxyType.values()) {
            if (proxyType.isAvailable(this)) {
                proxyTypes.add(proxyType);
            }
        }
    }
    
    List<ProxyType> proxyTypes() {
        return proxyTypes;
    }
}
