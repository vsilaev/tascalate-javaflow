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
package org.apache.commons.javaflow.instrumentation;

import org.apache.commons.javaflow.spi.Cache;
import org.apache.commons.javaflow.spi.ClasspathResourceLoader;
import org.apache.commons.javaflow.spi.MorphingResourceLoader;

import org.apache.commons.javaflow.providers.asmx.AsmxResourceTransformationFactory;

import org.apache.commons.javaflow.instrumentation.common.ConfigurableClassFileTransformer;

public class JavaFlowClassTransformer extends ConfigurableClassFileTransformer {
    private final Cache<ClassLoader, MorphingResourceLoader> cachedResourceLoaders = 
        new Cache<ClassLoader, MorphingResourceLoader>() {
            @Override
            protected MorphingResourceLoader createValue(ClassLoader classLoader) {
                MorphingResourceLoader loader = new MorphingResourceLoader(new ClasspathResourceLoader(classLoader));
                // "touch" factory with empty morph
                resourceTransformationFactory.createTransformer(loader).release();
                return loader;
            }
        };    
        
    public JavaFlowClassTransformer() {
        super(new AsmxResourceTransformationFactory());
    }

    @Override
    protected MorphingResourceLoader getResourceLoader(ClassLoader classLoader) {
        return cachedResourceLoaders.get(classLoader);
    }
}
