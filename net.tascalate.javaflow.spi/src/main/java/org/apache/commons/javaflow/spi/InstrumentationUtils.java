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
package org.apache.commons.javaflow.spi;

import java.util.HashSet;
import java.util.Set;

public final class InstrumentationUtils {
    
    private InstrumentationUtils() {}
    
    public static String packageNameOf(Class<?> clazz) {
        String className = clazz.getName();
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            return className.substring(0, lastDot);
        } else {
            return null;
        }
    }
    
    public static Set<String> packagePrefixesOf(Class<?>... classes) {
        Set<String> packagePrefixes = new HashSet<String>();
        for (Class<?> clazz : classes) {
            packagePrefixes.add( packageNameOf(clazz) + '.');
        }    
        return packagePrefixes;
    }
    
    
    public static String readClassName(byte[] bytes) {
        return new ClassHeaderReader(bytes).getClassName();
    }
    
    /**
     * Check if <code>maybeParent</code> is a parent (probably inderect) of the <code>classLoader</code>
     * @param classLoader The classloader whose parents are checked, may not be null
     * @param maybeParent Possible parent, may be null for boot class loader
     * @return
     */
    public static boolean isClassLoaderParent(ClassLoader classLoader, ClassLoader maybeParent) {
        ClassLoader cl = classLoader;
        do {
            cl = cl.getParent();
            if (maybeParent == cl) {
                // Check includes null == null for bootstrap classloader
                return true;
            }
        } while (cl != null);
        return false;
    }
    
}
