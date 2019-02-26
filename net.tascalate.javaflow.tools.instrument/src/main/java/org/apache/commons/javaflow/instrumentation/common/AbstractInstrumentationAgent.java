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
import java.lang.instrument.Instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.javaflow.spi.ClasspathResourceLoader;

public abstract class AbstractInstrumentationAgent {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    protected AbstractInstrumentationAgent() {
    }
    
    protected void install(String args, Instrumentation instrumentation) throws Exception {
        ClassFileTransformer transformer = createTransformer();
        instrumentation.addTransformer(transformer);
        System.setProperty(transformer.getClass().getName(), "true");
    }
    
    protected void attach(String args, Instrumentation instrumentation) throws Exception {
        log.info("Installing agent...");
        ClassFileTransformer transformer = createTransformer();
        instrumentation.addTransformer(transformer);
        if ("skip-retransform".equals(args)) {
            log.info("skip-retransform argument passed, skipping re-transforming classes");
        } else if (!instrumentation.isRetransformClassesSupported()) {
            log.info("JVM does not support re-transform, skipping re-transforming classes");
        } else {
            retransformClasses(instrumentation);
        }
        System.setProperty(transformer.getClass().getName(), "true");
        log.info("Agent was installed dynamically");
    }
    
    protected void retransformClasses(Instrumentation instrumentation) {
        log.info("Re-transforming existing classes...");
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            String className = clazz.getName();
            if (instrumentation.isModifiableClass(clazz)) {
                if (ClasspathResourceLoader.isClassLoaderParent(systemClassLoader, clazz.getClassLoader())) {
                    if (log.isTraceEnabled()) {
                        log.trace("Skip re-transforming boot or extension/platform class: " + className);
                    }
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Re-transforming class: " + className);
                }
                try {
                    instrumentation.retransformClasses(clazz);
                } catch (Throwable e) {
                    log.error("Error re-transofrming class "+ className, e);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Non-modifiable class (re-transforming skipped): " + className);
                }                    
            }
        }
        log.info("Existing classes was re-transormed");
    }
    
    protected abstract ClassFileTransformer createTransformer();
}