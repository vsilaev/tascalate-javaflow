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
package org.apache.commons.javaflow.instrumentation.common;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.javaflow.spi.ClasspathResourceLoader;
import org.apache.commons.javaflow.spi.MorphingResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;
import org.apache.commons.javaflow.spi.ResourceTransformer;

public abstract class ConfigurableClassFileTransformer implements ClassFileTransformer {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    
    protected final ResourceTransformationFactory resourceTransformationFactory;
    
    protected ConfigurableClassFileTransformer(ResourceTransformationFactory resourceTransformationFactory) {
        this.resourceTransformationFactory = resourceTransformationFactory;
    }
        
    // @Override
    public byte[] transform(ClassLoader classLoader, 
                            String className, 
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, 
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        
        if (isSystemClassLoaderParent(classLoader)) {
            if (log.isDebugEnabled()) {
                log.info("Ignoring class defined by boot or extensions/platform class loader: " + className);
            }
            return null;
        }

        // Ensure classLoader is not null (null for boot class loader)
        MorphingResourceLoader resourceLoader = getResourceLoader(getSafeClassLoader(classLoader));

        // Ensure className is not null (parameter is null for dynamically defined classes like lambdas)
        className = resolveClassName(className, classBeingRedefined, classfileBuffer);
        try {
            // Execute with current class as extra resource (in-memory)
            // Mandatory for Java8 lambdas and alike
            ResourceTransformer transformer = resourceTransformationFactory.createTransformer(
                resourceLoader.withReplacement(className + ".class", classfileBuffer)
            );
            try {
                return transformer.transform(classfileBuffer, className);
            } finally {
                transformer.release();
            }
        } catch (RuntimeException ex) {
            if (log.isErrorEnabled()) {
                if (VERBOSE_ERROR_REPORTS) {
                    log.error("Error transforming " + className, ex);
                } else {
                    log.error("Error transforming " + className);
                }
            }
            return null;
        } catch (ClassCircularityError ex) {
            if (VERBOSE_ERROR_REPORTS && log.isWarnEnabled()) {
                log.warn("Ignoring class circularity error: " + ex.getMessage());
            }
            return null;
        } catch (final Error ex) {
            log.error("Internal error during transforming continuable class", ex);
            throw ex;
        }
    }
    
    protected abstract MorphingResourceLoader getResourceLoader(ClassLoader classLoader);

    protected ClassLoader getSafeClassLoader(ClassLoader classLoader) {
        return null != classLoader ? classLoader : systemClassLoader;
    }

    private boolean isSystemClassLoaderParent(ClassLoader maybeParent) {
        return ClasspathResourceLoader.isClassLoaderParent(systemClassLoader, maybeParent);
    }
    
    private String resolveClassName(String className,
                                    Class<?> classBeingRedefined,
                                    byte[] classfileBuffer) {
        if (null != className) {
            return className;
        } else if (classBeingRedefined != null) {
            return classBeingRedefined.getName().replace('.', '/');
        } else {
            return resourceTransformationFactory.readClassName(classfileBuffer);
        }
    }

    private static final boolean VERBOSE_ERROR_REPORTS = 
        Boolean.getBoolean("org.apache.commons.javaflow.instrumentation.verbose");
}
