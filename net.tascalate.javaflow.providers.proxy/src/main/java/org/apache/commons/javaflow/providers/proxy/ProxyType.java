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
package org.apache.commons.javaflow.providers.proxy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.javaflow.providers.core.ContinuableClassInfo;
import org.apache.commons.javaflow.providers.proxy.cglib.CGLibProxyClassProcessor;
import org.apache.commons.javaflow.providers.proxy.custom.CustomProxyClassProcessor;
import org.apache.commons.javaflow.providers.proxy.jdk.JavaProxyClassProcessor;
import org.apache.commons.javaflow.providers.proxy.owb.OwbProxyClassProcessor;
import org.apache.commons.javaflow.providers.proxy.spring.SpringProxyClassProcessor;
import org.apache.commons.javaflow.providers.proxy.weld.WeldProxyClassProcessor;
import org.apache.commons.javaflow.spi.ResourceLoader;

import net.tascalate.asmx.plus.ClassHierarchy;

public enum ProxyType {
    
    WELD("org/jboss/weld/bean/proxy/ProxyObject") {
        
        @Override
        boolean accept(ClassHierarchy hierarchy, 
                       String className, 
                       String signature, 
                       String superName, 
                       String[] interfaces) {
            // *$$_WeldSubclass is not a scope/interceptor proxy
            // Otherwise it's indeed a proxy
            if (className.endsWith("$$_WeldSubclass")) {
                return false;
            }
            return super.accept(hierarchy, className, signature, superName, interfaces);
        }
        
        @Override
        ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
            return new WeldProxyClassProcessor(api, className, classInfo);
        }            

    },
    OWB(
        "org/apache/webbeans/proxy/OwbInterceptorProxy",
        "org/apache/webbeans/proxy/OwbNormalScopeProxy" 
        ) {
        
        @Override
        ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
            return new OwbProxyClassProcessor(api, className, classInfo);
        }
    },
    SPRING("org/springframework/aop/framework/Advised") {
        
        @Override
        ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
            return new SpringProxyClassProcessor(api, className, classInfo);
        }
        
    }, 
    CUSTOM_PROXY("org/apache/commons/javaflow/core/CustomContinuableProxy") {
        
        @Override
        ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
            return new CustomProxyClassProcessor(api, className, classInfo);
        }
    },
    CGLIB("org/apache/commons/javaflow/core/ContinuableProxy") {
        private final String cglibProxyBase = "net/sf/cglib/proxy/Proxy$ProxyImpl";
        
        @Override
        boolean accept(ClassHierarchy hierarchy, 
                       String className, 
                       String signature, 
                       String superName, 
                       String[] interfaces) {
            // Check that this is CGLib proxy
            if (cglibProxyBase.equals(superName) /*|| hierarchy.isSubClass(className, cglibProxyBase)*/) {
                return super.accept(hierarchy, className, signature, superName, interfaces);
            } else {
                return false;
            }
        }
        
        boolean isAvailable(ResourceLoader resourceLoader) {
            return resourceLoader.hasResource(cglibProxyBase + ".class") && super.isAvailable(resourceLoader);
        }
        
        @Override
        ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
            return new CGLibProxyClassProcessor(api, className, classInfo);
        }
    },
    JAVA("org/apache/commons/javaflow/core/ContinuableProxy") {
        private final String javaProxyBase = "java/lang/reflect/Proxy";
        
        @Override
        boolean accept(ClassHierarchy hierarchy, 
                       String className, 
                       String signature, 
                       String superName, 
                       String[] interfaces) {
            // Check that this is Java proxy
            if (javaProxyBase.equals(superName) /*|| hierarchy.isSubClass(className, cglibProxyBase)*/) {
                return super.accept(hierarchy, className, signature, superName, interfaces);
            } else {
                return false;
            }
        }        
        
        @Override
        ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo) {
            return new JavaProxyClassProcessor(api, className, classInfo);
        }     
    }
    ;

    private Set<String> markerInterfaces;
    
    private ProxyType(String... markerInterfaces) {
        this.markerInterfaces = 
            Collections.unmodifiableSet( new HashSet<String>(Arrays.asList(markerInterfaces)) );
    }
    
    boolean accept(ClassHierarchy hierarchy, 
                   String className, 
                   String signature, 
                   String superName, 
                   String[] interfaces) {
        // Fast route
        for (String intf : interfaces) {
            if (markerInterfaces.contains(intf)) {
                return true;
            }
        }
        // Safe route
        for (String intf : markerInterfaces) {
            if (hierarchy.isSubClass(className, intf)) {
                return true;
            }
        }
        return false;
    }
    
    boolean isAvailable(ResourceLoader resourceLoader) {
        return resourceLoader.hasResource(markerInterfaces.iterator().next() + ".class");
    }
    
    abstract ProxyClassProcessor createProcessor(int api, String className, ContinuableClassInfo classInfo);
}
