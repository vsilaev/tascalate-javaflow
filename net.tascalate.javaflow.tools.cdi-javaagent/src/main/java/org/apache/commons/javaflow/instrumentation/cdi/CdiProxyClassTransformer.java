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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tascalate.asmx.ClassReader;
import net.tascalate.asmx.ClassVisitor;
import net.tascalate.asmx.ClassWriter;

import org.apache.commons.javaflow.spi.Cache;
import org.apache.commons.javaflow.spi.ClasspathResourceLoader;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.MorphingResourceLoader;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;
import org.apache.commons.javaflow.spi.StopException;

import org.apache.commons.javaflow.providers.asmx.AsmxResourceTransformationFactory;
import org.apache.commons.javaflow.providers.asmx.ClassHierarchy;

public class CdiProxyClassTransformer implements ClassFileTransformer {
    private static final Logger log = LoggerFactory.getLogger(CdiProxyClassTransformer.class);

    private final ResourceTransformationFactory resourceTransformationFactory = new AsmxResourceTransformationFactory();
    private final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    private final Cache<ClassLoader, Object[]> cachedHelpers = 
        new Cache<ClassLoader, Object[]>() {
            @Override
            protected Object[] createValue(ClassLoader classLoader) {
                MorphingResourceLoader loader = new MorphingResourceLoader(
                    new ClasspathResourceLoader(classLoader)
                );
                
                // "touch" factory with empty morph
                resourceTransformationFactory.createResolver(loader).release();
                
                ClassHierarchy hierarchy = new ClassHierarchy(loader);
                List<ProxyType> proxyTypes = new ArrayList<ProxyType>();
                for (ProxyType proxyType : ProxyType.values()) {
                    if (proxyType.isAvailable(loader)) {
                        proxyTypes.add(proxyType);
                    }
                }
                return new Object[] {loader, hierarchy, proxyTypes};
            }
        };

    // @Override
    public byte[] transform(
            ClassLoader classLoader, 
            String className, 
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, 
            final byte[] classfileBuffer) throws IllegalClassFormatException {
        
        if (isSystemClassLoaderParent(classLoader)) {
            if (log.isDebugEnabled()) {
                log.info("Ignoring class defined by boot or extensions/platform class loader: " + className);
            }
            return null;
        }

        // Ensure classLoader is not null (null for boot class loader)
        Object[] helpers = cachedHelpers.get(getSafeClassLoader(classLoader));
        
        MorphingResourceLoader defaultLoader = (MorphingResourceLoader)helpers[0];
        ClassHierarchy sharedHierarchy = (ClassHierarchy)helpers[1];
        @SuppressWarnings("unchecked")
        List<ProxyType> proxyTypes = (List<ProxyType>)helpers[2];

        // Ensure className is not null (parameter is null for dynamically defined classes like lambdas)
        className = resolveClassName(className, classBeingRedefined, classfileBuffer);
        try {
            // Execute with current class as extra resource (in-memory)
            // Mandatory for Java8 lambdas and alike
            ResourceLoader actualLoader = defaultLoader.withReplacement(className + ".class", classfileBuffer);
            ClassHierarchy actualHierarchy = sharedHierarchy.shareWith(actualLoader);
            ContinuableClassInfoResolver resolver = resourceTransformationFactory.createResolver(actualLoader);
            
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
            try {
                ClassVisitor adapter = new CdiProxyClassAdapter(writer, resolver, actualHierarchy, proxyTypes);  
                reader.accept(adapter, ClassReader.EXPAND_FRAMES);
            } finally {
                resolver.release();
            }
            return writer.toByteArray();
        } catch (StopException ex) {
            return null;
        } catch (RuntimeException ex) {
            if (log.isErrorEnabled()) {
                if (VERBOSE_ERROR_REPORTS) {
                    log.error("Error transforming " + className, ex);
                } else {
                    log.error("Error transforming " + className);
                }
            }
            return null;
        } catch (Error ex) {
            log.error("Internal error during transforming CDI continuable proxy", ex);
            throw ex;
        }
    }

    protected ClassLoader getSafeClassLoader(final ClassLoader classLoader) {
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

    private static final boolean VERBOSE_ERROR_REPORTS = Boolean.getBoolean("org.apache.commons.javaflow.instrumentation.verbose");
}
