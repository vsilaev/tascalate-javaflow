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
package org.apache.commons.javaflow.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class ClassMatchStrategies {
    private ClassMatchStrategies() {}
    
    public static final ClassMatchStrategy MATCH_NONE = new ClassMatchStrategy() {
        @Override
        public boolean matches(String name, String signature, String superName, String[] interfaces, ResourceLoader loader) {
            return false;
        }
        
        @Override
        public ClassMatcher bind(ResourceLoader ignore) {
            return ClassMatcher.MATCH_NONE;
        }
    };
    
    public static final ClassMatchStrategy MATCH_ALL = new ClassMatchStrategy() {
        @Override
        public boolean matches(String name, String signature, String superName, String[] interfaces, ResourceLoader loader) {
            return true;
        }
        
        @Override
        public ClassMatcher bind(ResourceLoader ignore) {
            return ClassMatcher.MATCH_ALL;
        }
    };
    
    public static ClassMatchStrategy whenAll(ClassMatchStrategy... matchers) {
        return whenAll(Arrays.asList(matchers));
    }
    
    public static ClassMatchStrategy whenAll(final Collection<? extends ClassMatchStrategy> matchers) {
        return new ClassMatchStrategy() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces, ResourceLoader loader) {
                for (ClassMatchStrategy m : matchers) {
                    if (!m.matches(name, signature, superName, interfaces, loader)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }
    
    public static ClassMatchStrategy whenAny(ClassMatchStrategy... matchers) {
        return whenAny(Arrays.asList(matchers));
    }
    
    public static ClassMatchStrategy whenAny(final Collection<? extends ClassMatchStrategy> matchers) {
        return new ClassMatchStrategy() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces, ResourceLoader loader) {
                for (ClassMatchStrategy m : matchers) {
                    if (m.matches(name, signature, superName, interfaces, loader)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
    
    public static ClassMatchStrategy negate(final ClassMatchStrategy matcher) {
        return new ClassMatchStrategy() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces, ResourceLoader loader) {
                return !matcher.matches(name, signature, superName, interfaces, loader);
            }
        };
    }
    
    public static ClassMatchStrategy byClassName(String className, final boolean namePart) {
        final String cn = className(className);
        return new ClassMatchStrategy() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces, ResourceLoader loader) {
                return namePart && name.contains(cn) || name.equals(cn);
            }
        };
    }    
    
    public static ClassMatchStrategy byClassNamePattern(final String classNamePattern) {
        final Pattern pattern = Pattern.compile("^" + classNamePattern + "$");
        return new ClassMatchStrategy() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces, ResourceLoader loader) {
                return pattern.matcher(name).matches();
            }
        };        
    }
    
    public static ClassMatchStrategy bySuperClass(final ClassMatchStrategy nested) {
        return new ClassMatchStrategy() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces, ResourceLoader loader) {
                String cname = superName;
                while (null != cname && cname.length() > 0) {
                    ClassHeaderReader chr;
                    try {
                        chr = getClassHeader(loader, cname);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    String nextSuperName = chr.getSuperName();
                    if (nested.matches(cname, null, nextSuperName, chr.getInterfaces(), loader)) {
                        return true;
                    }
                    cname = nextSuperName;
                }
                return false;
            }
        };
    }
    
    public static ClassMatchStrategy byInterface(final ClassMatchStrategy nested) {
        return new ClassMatchStrategy() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces, ResourceLoader loader) {
                if (null == interfaces || interfaces.length == 0) {
                    return false;
                }
                Set<String> visited = new HashSet<String>();
                for (String intf : interfaces) {
                    if (matchInterface(intf, nested, visited, loader)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
    
    private static boolean matchInterface(String intf, ClassMatchStrategy nested, Set<String> visited, ResourceLoader loader) {
        if (visited.contains(intf)) {
            return false;
        }
        ClassHeaderReader chr;
        try {
            chr = getClassHeader(loader, intf);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        if (nested.matches(intf, null, chr.getSuperName(), chr.getInterfaces(), loader)) {
            return true;
        }
        visited.add(intf);
        String[] nextInterfaces = chr.getInterfaces();
        for (String nextInterface : nextInterfaces) {
            if (matchInterface(nextInterface, nested, visited, loader)) {
                return true;
            }
        }
        return false;
    }
    
    private static String className(String internalClassName) {
        return internalClassName != null ? internalClassName.replace('.', '/') : null;
    }
    
    private static ClassHeaderReader getClassHeader(ResourceLoader loader, String className) throws IOException {
        return new ClassHeaderReader(getClassBytes(loader, className));
    }
    
    private static byte[] getClassBytes(ResourceLoader loader, String className) throws IOException, SecurityException {
        InputStream in = loader.getResourceAsStream(className + ".class");
        try {
            return getStreamBytes(in);
        } finally {
            if (null != in) { 
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
    
    private static byte[] getStreamBytes(InputStream stream) throws IOException {
        int BUFFER_SIZE = 4096;
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream(BUFFER_SIZE);
        try {

            int bytesRead;
            byte[] buffer = new byte[BUFFER_SIZE];

            while ((bytesRead = stream.read(buffer, 0, BUFFER_SIZE)) > 0) {
                baos.write(buffer, 0, bytesRead);
            }
            
            byte[] data = baos.unsafeBytes();
            return data;

        } finally {
            baos.close();
        }
    }
}
