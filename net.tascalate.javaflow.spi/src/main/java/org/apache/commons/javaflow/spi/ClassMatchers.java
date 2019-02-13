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
package org.apache.commons.javaflow.spi;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

public final class ClassMatchers {
    private ClassMatchers() {}
    
    public static ClassMatcher MATCH_NONE = new ClassMatcher() {
        @Override
        public boolean matches(String name, String signature, String superName, String[] interfaces) {
            return false;
        }
    };
    
    public static ClassMatcher MATCH_ALL = new ClassMatcher() {
        @Override
        public boolean matches(String name, String signature, String superName, String[] interfaces) {
            return true;
        }
    };

    
    public static ClassMatcher whenAll(ClassMatcher... matchers) {
        return whenAll(Arrays.asList(matchers));
    }
    
    public static ClassMatcher whenAll(final Collection<? extends ClassMatcher> matchers) {
        return new ClassMatcher() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces) {
                for (ClassMatcher m : matchers) {
                    if (!m.matches(name, signature, superName, interfaces)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }
    
    public static ClassMatcher whenAny(ClassMatcher... matchers) {
        return whenAny(Arrays.asList(matchers));
    }
    
    public static ClassMatcher whenAny(final Collection<? extends ClassMatcher> matchers) {
        return new ClassMatcher() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces) {
                for (ClassMatcher m : matchers) {
                    if (m.matches(name, signature, superName, interfaces)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
    
    public static ClassMatcher negate(final ClassMatcher matcher) {
        return new ClassMatcher() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces) {
                return !matcher.matches(name, signature, superName, interfaces);
            }
        };
    }
    
    public static ClassMatcher byClassName(String className, final boolean namePart) {
        final String cn = className(className);
        return new ClassMatcher() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces) {
                return namePart && name.equals(cn) || name.contains(cn);
            }
        };
    }    
    
    public static ClassMatcher byClassNamePattern(final String classNamePattern) {
        final Pattern pattern = Pattern.compile("^" + escapeDots(className(classNamePattern)) + "$");
        return new ClassMatcher() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces) {
                return pattern.matcher(name).matches();
            }
        };        
    }
    
    public static ClassMatcher bySuperClassName(String superClassName, final boolean namePart) {
        return bySuperClass(byClassName(superClassName, namePart));
    }
    
    public static ClassMatcher bySuperClassNamePattern(String superClassNamePattern) {
        return bySuperClass(byClassNamePattern(superClassNamePattern));
    }    
    
    public static ClassMatcher byInterfaceName(String interfaceName, final boolean namePart) {
        return byInterface(byClassName(interfaceName, namePart));
    }
    
    public static ClassMatcher byInterfaceNamePattern(String interfaceNamePattern) {
        return byInterface(byClassNamePattern(interfaceNamePattern));
    }
    
    private static ClassMatcher bySuperClass(final ClassMatcher nested) {
        return new ClassMatcher() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces) {
                return nested.matches(superName, null, null, null);
            }
        };
    }
    
    private static ClassMatcher byInterface(final ClassMatcher nested) {
        return new ClassMatcher() {
            @Override
            public boolean matches(String name, String signature, String superName, String[] interfaces) {
                if (null != interfaces) {
                    for (String intf : interfaces) {
                        if (nested.matches(intf, null, null, null)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }
    
    private static String escapeDots(String s) {
        return s != null ? MATCH_DOTS.matcher(s).replaceAll("\\.") : null;
    }
    
    private static String className(String internalClassName) {
        return internalClassName != null ? internalClassName.replace('/', '.') : null;
    }
    
    private static final Pattern MATCH_DOTS = Pattern.compile("\\.");
}
