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

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClasspathResourceLoader implements VetoableResourceLoader {

    private final Reference<ClassLoader> classLoaderRef;
    
    public ClasspathResourceLoader(ClassLoader classLoader) {
        this.classLoaderRef = new WeakReference<ClassLoader>(classLoader);
    }

    public boolean hasResource(String name) {
        ClassLoader classLoader = classLoaderRef.get();
        return null != classLoader && null != classLoader.getResource(name);
    }
    
    public InputStream getResourceAsStream(String name) throws IOException {
        ClassLoader classLoader = classLoaderRef.get();
        if (null == classLoader) {
            throw new IOException("Underlying class loader was evicted from memory, this resource loader is unusable");
        }
        
        InputStream result = classLoader.getResourceAsStream(name);
        if (null == result) {
            throw new IOException("Unable to find resource " + name);
        }
        return result;
    }
    
    public ClassMatcher createVeto() throws IOException {
        List<ClassMatcher> matchers = new ArrayList<ClassMatcher>();
        ClassLoader classLoader = classLoaderRef.get();
        if (null == classLoader) {
            return ClassMatchers.MATCH_NONE;
        }
        Enumeration<URL> allResources = classLoader.getResources("META-INF/net.tascalate.javaflow.veto.cmf");
        ClassMatcherFileParser parser = new ClassMatcherFileParser();
        while (allResources.hasMoreElements()) {
            URL resource = allResources.nextElement();
            ClassMatcher matcher = parser.parse(resource);
            if (null != matcher && ClassMatchers.MATCH_NONE != matcher) {
                matchers.add(matcher);
            }
        }
        return matchers.isEmpty() ? ClassMatchers.MATCH_NONE : ClassMatchers.whenAny(matchers);
    }
}
