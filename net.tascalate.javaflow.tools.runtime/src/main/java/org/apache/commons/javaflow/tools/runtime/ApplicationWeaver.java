/**
 * ﻿Original work: copyright 1999-2004 The Apache Software Foundation
 * (http://www.apache.org/)
 *
 * This project is based on the work licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Modified work: copyright 2013-2022 Valery Silaev (http://vsilaev.com)
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.javaflow.spi.InstrumentationUtils;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;

public final class ApplicationWeaver {
    private ApplicationWeaver() {}
    
    public static boolean bootstrap(ResourceTransformationFactory factory, String...args) {
        return _trampoline(factory, null, null, null, args);
    }
    
    public static boolean bootstrap(ResourceTransformationFactory factory, 
                                    boolean isContinuablePackageRoot, 
                                    String...args) {
        return _trampoline(factory, null, null, asPackageRoots(isContinuablePackageRoot), args); 
    }
    
    public static boolean bootstrap(ResourceTransformationFactory factory, 
                                    String[] continuablePackageRoots, 
                                    String...args) {
        return _trampoline(factory, null, null, Arrays.asList(continuablePackageRoots), args); 
    }
    
    public static boolean bootstrap(ResourceTransformationFactory factory, 
                                    Collection<String> continuablePackageRoots, 
                                    String...args) {
        return _trampoline(factory, null, null, continuablePackageRoots, args); 
    }    
    
    public static boolean bootstrap(ResourceTransformationFactory factory, ClassLoader loader, String...args) {
        return _trampoline(factory, null, loader, null, args);
    }
    
    public static boolean bootstrap(ResourceTransformationFactory factory, 
                                    ClassLoader loader, 
                                    boolean isContinuablePackageRoot,
                                    String...args) {
        return _trampoline(factory, null, loader, asPackageRoots(isContinuablePackageRoot), args);
    }
    
    public static boolean bootstrap(ResourceTransformationFactory factory, 
                                    ClassLoader loader, 
                                    String[] continuablePackageRoots,
                                    String...args) {
        return _trampoline(factory, null, loader, Arrays.asList(continuablePackageRoots), args);
    }    
    
    public static boolean bootstrap(ResourceTransformationFactory factory,
                                    ClassLoader loader,            
                                    Collection<String> continuablePackageRoots, 
                                    String...args) {
        return _trampoline(factory, null, null, continuablePackageRoots, args); 
    }      
    
    public static boolean bootstrap(ResourceTransformationFactory factory, Class<?> source, String...args) {
        return _trampoline(factory, source, source.getClassLoader(), null, args);
    }
    
    public static boolean bootstrap(ResourceTransformationFactory factory, 
                                    Class<?> source, 
                                    boolean isContinuablePackageRoot,
                                    String...args) {
        return _trampoline(factory, source, source.getClassLoader(), asPackageRoots(isContinuablePackageRoot), args);
    }
    
    
    public static boolean bootstrap(ResourceTransformationFactory factory, 
                                    Class<?> source, 
                                    String[] continuablePackageRoots,
                                    String...args) {
        return _trampoline(factory, source, source.getClassLoader(), Arrays.asList(continuablePackageRoots), args);
    }
    
    
    public static boolean bootstrap(ResourceTransformationFactory factory, 
                                    Class<?> source, 
                                    Collection<String> continuablePackageRoots,
                                    String...args) {
        return _trampoline(factory, source, source.getClassLoader(), continuablePackageRoots, args);
    }
    
    private static boolean _trampoline(ResourceTransformationFactory factory, 
                                       Class<?> originalClass, 
                                       ClassLoader originalLoader,
                                       Collection<String> continuablePackageRoots,
                                       String...args) {
        if (null == originalLoader) {
            if (null != originalClass) {
                originalLoader = originalClass.getClassLoader();
            } else {
                originalLoader = CURRENT_INITIATOR.get();
                if (null == originalLoader) {
                    originalLoader = Thread.currentThread().getContextClassLoader();
                }
                if (null == originalLoader) {
                    originalLoader = ClassLoader.getSystemClassLoader();
                }                
            }
        }
        
        if (null == originalClass) {
            StackTraceElement ste = new Exception().getStackTrace()[2];
            try {
                originalClass = originalLoader.loadClass(ste.getClassName());
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(
                    "Unable to load requesting class " + ste.getClassName() + " using class loader " + originalLoader,
                    ex
                );
            }
        }
        
        if ((originalLoader instanceof ResourceTransformingClassLoader) &&
            isProcessed(originalClass)) {
            // Correct loader and annotations applied
            return false;
        }
        
        if (null != CURRENT_INITIATOR.get()) {
            // In recursion, without exception it will be infinite
            throw new IllegalStateException("Class " + originalClass.getName() + " is not instrumented, probably it has no continuable methods");
        }
        
        try {
            ResourceTransformingClassLoader.Builder builder = new ResourceTransformingClassLoader.Builder(factory);
            if (null == continuablePackageRoots || continuablePackageRoots.isEmpty()) {
                builder.parentFirst(false);
            } else {
                Set<String> filteredPackageRoots = new HashSet<String>(continuablePackageRoots);
                for (String s : filteredPackageRoots) {
                    if ("*".equals(s)) {
                        s = InstrumentationUtils.packageNameOf(originalClass);
                    }
                    builder.addLoaderPackageRoot(s);
                }
            }
            ResourceTransformingClassLoader loader = builder.parent(originalLoader).create();
            
            CURRENT_INITIATOR.set(loader);
            try {
                run(loader, originalClass.getName(), "main", args);
            } finally {
                CURRENT_INITIATOR.remove();
            }
        }
        catch (RuntimeException ex) { 
            throw ex; 
        }
        catch (Exception ex) { 
            throw new RuntimeException(ex); 
        }
        return true;
    }
    
    private static void run(ResourceTransformingClassLoader classLoader, String className, String method, String... args) throws Exception {
        Class<?> mainClass = classLoader.forceLoadClass(className);
        if (!isProcessed(mainClass)) {
            throw new IllegalStateException("Class " + className + " has no continuable methods");
        }
        Method mainMethod = mainClass.getMethod(method, String[].class);
        mainMethod.invoke(null, new Object[] {args});
    }
    
    private static boolean isProcessed(Class<?> clazz) {
        for (Annotation a : clazz.getAnnotations()) {
            if ("org.apache.commons.javaflow.core.Skip".equals(a.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }
    
    private static Collection<String> asPackageRoots(boolean isContinuablePackageRoot) {
        return isContinuablePackageRoot ? Collections.singleton("*") : null;
    }

    private static final ThreadLocal<ClassLoader> CURRENT_INITIATOR = new ThreadLocal<ClassLoader>();
}
