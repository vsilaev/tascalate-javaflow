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
package org.apache.commons.javaflow.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

public class MorphingResourceLoader implements VetoableResourceLoader {
    protected final ResourceLoader delegate;
    private final Map<String, byte[]> extraResources;

    public MorphingResourceLoader(ResourceLoader delegate) {
        this(delegate, Collections.<String, byte[]>emptyMap());
    }
    
    public MorphingResourceLoader(ResourceLoader delegate, Map<String, byte[]> extraResources) {
        this.delegate = delegate;
        this.extraResources = null != extraResources ? 
            Collections.unmodifiableMap(extraResources) : Collections.<String, byte[]>emptyMap();
    }
    
    public final ResourceLoader withReplacement(String altResourceName, byte[] altResourceContent) {
        return withReplacement(Collections.singletonMap(altResourceName, altResourceContent));
    }
    
    public ResourceLoader withReplacement(Map<String, byte[]> alternativeResources) {
        return new MorphingResourceLoader(delegate, alternativeResources);
    }
    
    public final ResourceLoader withAddition(String addResourceName, byte[] addResourceContent) {
        return withAddition(Collections.singletonMap(addResourceName, addResourceContent));
    }
    
    public ResourceLoader withAddition(Map<String, byte[]> additionalResources) {
        return new MorphingResourceLoader(this, additionalResources);
    }
    
    @Override
    public boolean hasResource(String name) {
        return extraResources.containsKey(name) || delegate.hasResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) throws IOException {
        byte[] result = extraResources.get(name);
        if (null != result) {
            return new FastByteArrayInputStream(result);
        } else {
            return delegate.getResourceAsStream(name);
        }
    }
    
    @Override
    public ClassMatcher createVeto() throws IOException {
        return getVetoStrategy().bind(this);
    }    

    @Override
    public ClassMatchStrategy getVetoStrategy() throws IOException {
        if (delegate instanceof VetoableResourceLoader) {
            return ((VetoableResourceLoader)delegate).getVetoStrategy();
        } else {
            return ClassMatchStrategies.MATCH_NONE;
        }
    }
    
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (null == other || other.getClass() != this.getClass()) {
            return false;
        }
        return delegate.equals(((MorphingResourceLoader)other).delegate);
    }
  
}
