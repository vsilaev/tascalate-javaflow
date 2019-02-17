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
package org.apache.commons.javaflow.providers.asmx;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.tascalate.asmx.ClassReader;
import net.tascalate.asmx.Opcodes;
import net.tascalate.asmx.Type;

import org.apache.commons.javaflow.spi.ResourceLoader;

/**
 * A class that computes the common super class of two classes without
 * actually loading them with a ClassLoader.
 * 
 * @author Eric Bruneton
 * @author vsilaev
 */
public class ClassHierarchy {
    
    private final ResourceLoader loader;
    private final Map<Key, String> lookupCache = new HashMap<Key, String>();
    private final Map<TypeInfo, Reference<TypeInfo>> typesCache 
        = new WeakHashMap<TypeInfo, Reference<TypeInfo>>();
    
    public ClassHierarchy(ResourceLoader loader) {
        this.loader = loader;
        typesCache.put(OBJECT, new WeakReference<TypeInfo>(OBJECT));
    }

    public boolean isSubClass(String type1, String type2) {
        String commonSuperClass = getCommonSuperClass(type1, type2);
        return type2.equals(commonSuperClass);
    }
    
    public boolean isSuperClass(String type1, String type2) {
        // Biased towards isSublass logic while 
        // calculateCommonSuperClass is optimized this way
        return isSubClass(type2, type1);
    }

    public String getCommonSuperClass(String type1, String type2) {
        Key key = new Key(type1, type2);
        String result;
        synchronized (lookupCache) {
            result = lookupCache.get(key);
            if (null == result) {
                result = calculateCommonSuperClass(type1, type2);
                lookupCache.put(key, result);
            }
        }
        return result;
    }
    
    Type getCommonSuperType(Type type1, Type type2) {
        return Type.getObjectType(getCommonSuperClass(type1.getInternalName(), type2.getInternalName()));
    }
    
    private String calculateCommonSuperClass(final String type1, final String type2) {
        try {
            TypeInfo info1 = getTypeInfo(type1);
            TypeInfo info2 = getTypeInfo(type2);
            // Fast check without deep loading of info2
            if (info1.implementationOf(info2)) {
                return type2;
            }
            // The reverse, now both will be loaded
            if (info2.implementationOf(info1)) {
                return type1;
            }
            List<TypeInfo> supers1 = info1.flattenHierarchy();
            List<TypeInfo> supers2 = info2.flattenHierarchy();
            // Matching from the most specific to least specific
            for (TypeInfo a : supers1) {
                for (TypeInfo b : supers2) {
                    if (a.equals(b)) {
                        return a.name;
                    }
                }
            }
            return OBJECT.name;
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }
    
    TypeInfo getTypeInfo(String type) throws IOException {
        TypeInfo key = new TypeInfo(type, null, null, false);
        synchronized (typesCache) {
            Reference<TypeInfo> reference = typesCache.get(key); 
            TypeInfo value = null != reference ? reference.get() : null;
            if (null == value) {
                ClassReader info = loadTypeInfo(type);
                value = new TypeInfo(info.getClassName(), 
                                     info.getSuperName(), 
                                     info.getInterfaces(),
                                     (info.getAccess() & Opcodes.ACC_INTERFACE) != 0);
                // Same key & value
                typesCache.put(value, new WeakReference<TypeInfo>(value)); 
            }
            return value;
        }
    }

    /**
     * Returns a ClassReader corresponding to the given class or interface.
     * 
     * @param type
     *            the internal name of a class or interface.
     * @return the ClassReader corresponding to 'type'.
     * @throws IOException
     *             if the bytecode of 'type' cannot be loaded.
     */
    private ClassReader loadTypeInfo(String type) throws IOException {
        InputStream is = loader.getResourceAsStream(type + ".class");
        try {
            return new ClassReader(is);
        } finally {
            is.close();
        }
    }
    
    class TypeInfo {
        final String name;
        final boolean isInterface;
        
        private String superClassName;
        private TypeInfo superClass;
        
        private String[] interfaceNames;
        private TypeInfo[] interfaces;
        
        TypeInfo(String name, String superClassName, String[] interfaceNames, boolean isInterface) {
            this.name = name;
            this.isInterface = isInterface;
            this.superClassName = superClassName;
            this.interfaceNames = null != interfaceNames ? interfaceNames : new String[0];
        }
        
        synchronized TypeInfo superClass() throws IOException {
            if (null != superClassName) {
                // Not loaded yet
                superClass = getTypeInfo(superClassName);
                superClassName = null;
            }
            return superClass;
        }
        
        synchronized TypeInfo[] interfaces() throws IOException {
            if (null != interfaceNames) {
                // Not loaded yet
                // For flatten we will need a predictable order
                Arrays.sort(interfaceNames);
                int size = interfaceNames.length;
                interfaces = new TypeInfo[size];
                for (int i = 0; i < size; i++) {
                    interfaces[i] = getTypeInfo(interfaceNames[i]);
                }
                interfaceNames = null;
            }
            return interfaces;
        }
        
        boolean implementationOf(TypeInfo base) throws IOException {
            String targetName = base.name;
            // Check names first to avoid loading hierarchy
            if (name.equals(targetName)) {
                return true;
            }
            synchronized (this) {
                if ((!base.isInterface) && 
                    null != superClassName && 
                    superClassName.equals(targetName)) {
                    return true;
                }
                if (base.isInterface && null != interfaceNames) {
                    int size = interfaceNames.length;
                    for (int i = 0; i < size; i++) {
                        if (interfaceNames[i].equals(targetName)) {
                            return true;
                        }
                    }
                }
            }
            TypeInfo t = superClass();
            if (null != t && t.implementationOf(base)) {
                return true;
            }
            // If base is interface then check interfaces
            if (base.isInterface) {
                TypeInfo[] tt = interfaces();
                int size = tt.length;
                for (int i = 0; i < size; i++) {
                    if (tt[i].implementationOf(base)) {
                        return true;
                    }
                }
            }
            return false;            
        }
        
        List<TypeInfo> flattenHierarchy() throws IOException {
            List<TypeInfo> bySuperclass = new LinkedList<TypeInfo>();
            List<TypeInfo> byInterfaces = new LinkedList<TypeInfo>();
            flattenSuperclassHierarchy(bySuperclass);
            flattenInterfacesHierarchy(byInterfaces);
            List<TypeInfo> result = new ArrayList<TypeInfo>(bySuperclass.size() + 
                                                            byInterfaces.size() +
                                                            1);
            result.addAll(bySuperclass);
            result.addAll(byInterfaces);
            result.add(OBJECT);
            return result;
        }
        
        private void flattenSuperclassHierarchy(List<TypeInfo> result) throws IOException {
            TypeInfo t = this;
            while ((t = t.superClass()) != null) {
                if (t.superClass() == null) {
                    // Do not include java.lang.Object
                    break;
                }
                result.add(t);
            }
        }
        
        private void flattenInterfacesHierarchy(List<TypeInfo> result) throws IOException {
            TypeInfo[] tt = interfaces();
            int size = tt.length;
            for (int i = size - 1; i >= 0; i--) {
                TypeInfo t = tt[i];
                // From bottom to top
                t.flattenInterfacesHierarchy(result);
                if (!result.contains(t)) {
                    // skip if re-implemented on higher level
                    result.add(0, t);
                }
            }
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        @Override
        public int hashCode() {
            return name.hashCode();
        }
        
        @Override
        public boolean equals(Object other) {
            if ((null == other) || !(other instanceof TypeInfo)) {
                return false;
            }
            return name.equals(((TypeInfo)other).name);
        }
    }
    
    private final TypeInfo OBJECT = new TypeInfo("java/lang/Object", null, null, false) {
        @Override
        TypeInfo superClass() {
            return null;
        }
    };
    
    static class Key extends AmbivalentDuoKey<String> {
        Key(String a, String b) {
            super(a, b);
        }
    }
    
    static class AmbivalentDuoKey<T> {
        private final T a;
        private final T b;
        AmbivalentDuoKey(T a, T b) {
            this.a = a;
            this.b = b;
        }
        
        @Override 
        public int hashCode() {
            int hA = null == a ? 0 : a.hashCode();
            int hB = null == b ? 0 : b.hashCode();
            return Math.min(hA, hB) * 37 + Math.max(hA, hB);
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (other.getClass() != this.getClass()) {
                return false;
            }
            @SuppressWarnings("unchecked")
            AmbivalentDuoKey<T> that = (AmbivalentDuoKey<T>)other;
            return same(this.a, that.a) && same(this.b, that.b) ||
                   same(this.a, that.b) && same(this.b, that.a);  
        }
        
        private static <T> boolean same(T a, T b) {
            return a == null ? b == null : a.equals(b);
        }
    }
}
