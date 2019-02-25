/**
 * ﻿Original work: copyright 1999-2004 The Apache Software Foundation
 * (http://www.apache.org/)
 *
 * This project is based on the work licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Modified work: copyright 2013-2019 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.tools.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.javaflow.spi.ResourceTransformationFactory;

public final class ApplicationLoader {
    private ApplicationLoader() {}
    
    public static boolean trampoline(ResourceTransformationFactory factory, Object source, String...args) {
        StackTraceElement ste = new Exception().getStackTrace()[1];
        Class<?> clazz;
        ClassLoader originalLoader;
        
        if (null == source) {
            clazz = null;
            originalLoader = Thread.currentThread().getContextClassLoader();
            if (null == originalLoader) {
                originalLoader = ClassLoader.getSystemClassLoader();
            }
        } else if (source instanceof Class) {
            clazz = (Class<?>)source;
            originalLoader = clazz.getClassLoader();
        } else {
            clazz = source.getClass();
            originalLoader = clazz.getClassLoader();
        }
                
        if (originalLoader instanceof ContinuableClassLoader)
            return false;

        try {
            try {
                Class<?> check = null != clazz ? clazz : originalLoader.loadClass(ste.getClassName());
                for (Annotation a : check.getAnnotations()) {
                    if ("org.apache.commons.javaflow.core.Skip".equals(a.getClass().getName())) {
                        return false;
                    }
                }
            } catch (ClassNotFoundException ex) {
            }
            
            ContinuableClassLoader loader = new ContinuableClassLoader.Builder(factory)
                                                                      .parent(originalLoader)
                                                                      .parentFirst(false)
                                                                      //.addLoaderPackageRoot(clazz.getPackage().getName())
                                                                      .create();
            
            run(loader, ste.getClassName(), ste.getMethodName(), args);
        }
        catch (RuntimeException ex) { 
            throw ex; 
        }
        catch (Exception ex) { 
            throw new RuntimeException(ex); 
        }
        return true;
    }
    
    private static void run(ContinuableClassLoader classLoader, String className, String method, String... args) throws Exception {
        Class<?> mainClass = classLoader.forceLoadClass(className);
        Method mainMethod = mainClass.getMethod(method, String[].class);
        mainMethod.invoke(null, new Object[] {args});
    }

}
