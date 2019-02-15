/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.javaflow.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tcurdt
 *
 */
public final class ReflectionUtils {
    
    private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);
    
    public interface Matcher {
        boolean matches(String pName);
    }

    public interface Indexer {
        void put(Map<String, Object> pMap, String pKey, Object pObject);
    }

    private static Indexer defaultIndexer = new DefaultIndexer();
    private static Matcher defaultMatcher = new DefaultMatcher();

    public static class DefaultMatcher implements Matcher {
        public boolean matches(String pName) {
            return true;
        }
    }

    public static class DefaultIndexer implements Indexer {
        public void put(Map<String, Object> pMap, String pKey, Object pObject) {
            pMap.put(pKey, pObject);
        }
    }

    public static Map<String, Object> discoverFields(Class<?> pClazz) {
        return discoverFields(pClazz, defaultMatcher, defaultIndexer);
    }
    
    public static Map<String, Object> discoverFields(Class<?> pClazz,
                                                     Matcher pMatcher) {
        return discoverFields(pClazz, pMatcher, defaultIndexer);
    }
    
    public static Map<String, Object> discoverFields(Class<?> pClazz,
                                                     Matcher pMatcher,
                                                     Indexer pIndexer) {
        log.debug("discovering fields on " + pClazz.getName());
        
        Map<String, Object> result = new HashMap<String, Object>();

        Class<?> current = pClazz;
        do {
            Field[] fields = current.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                String fname = fields[i].getName();
                if (pMatcher.matches(fname)) {
                    pIndexer.put(result, fname, fields[i]);
                    
                    log.debug("discovered field " + fname + " -> " + fields[i]);
                }
            }
            current = current.getSuperclass();
        } while(current != null);
     
        return result;
    }    

    public static Map<String, Object> discoverMethods(Class<?> pClazz) {
        
        return discoverMethods(pClazz, defaultMatcher, defaultIndexer);
    }
    
    public static Map<String, Object> discoverMethods(Class<?> pClazz,
                                                      Matcher pMatcher) {
        return discoverMethods(pClazz, pMatcher, defaultIndexer);
    }
    
    public static Map<String, Object> discoverMethods(Class<?> pClazz,
                                                      Matcher pMatcher,
                                                      Indexer pIndexer) {
        
        log.debug("discovering methods on " + pClazz.getName());
        
        Map<String, Object> result = new HashMap<String, Object>();

        Class<?> current = pClazz;
        do {
            Method[] methods = current.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                String mname = methods[i].getName();
                if (pMatcher.matches(mname)) {
                    pIndexer.put(result, mname, methods[i]);

                    log.debug("discovered method " + mname + " -> " + methods[i]);
                }
            }
            current = current.getSuperclass();
        } while(current != null);
     
        return result;
    }    

    public static Object clone(Object o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(buffer);
        try {
            oos.writeObject(o);
            oos.flush();
        } finally {
            oos.close();
        }
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        try {
            return ois.readObject();
        } finally {
            ois.close();
        }
      }
    
    public static String getClassName(final Object o) {
        if (o == null) {
            return "unknown";
        } else {
            return o.getClass().getName() + "@" + o.hashCode();
        }
    }

    public static String getClassLoaderName(final Object o) {
        if (o == null) {
            return "unknown";
        } else {
            return getClassName(o.getClass().getClassLoader());
        }
    }
    
    public static final String descriptionOfObject(Object o) {
        return getClassName(o) + "/" + getClassLoaderName(o) + " [" + o + "]";
    }
    
    public static final String descriptionOfClass(Object o) {
        return getClassName(o) + "/" + getClassLoaderName(o);
    }
}
