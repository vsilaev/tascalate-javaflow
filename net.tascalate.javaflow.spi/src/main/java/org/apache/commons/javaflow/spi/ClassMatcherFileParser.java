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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ClassMatcherFileParser {
    ClassMatcher parse(URL url) throws IOException {
        List<ClassMatcher> matchers = new ArrayList<ClassMatcher>();
        BufferedReader reader = new LineNumberReader(new InputStreamReader(url.openStream()));
        try {
            String s; 
            while (null != (s = reader.readLine())) {
                if (s.length() == 0 || s.startsWith("#")) {
                    continue;
                }
                int columnPos = s.indexOf(':');
                int equalsPos = s.indexOf('=');
                if (columnPos < 0 || equalsPos < 0) {
                    // Invalid line
                    continue;
                }
                String kind    = s.substring(0, columnPos).trim();
                String variant = s.substring(columnPos + 1, equalsPos).trim();
                String target  = s.substring(equalsPos + 1).trim();
                if (target.length() == 0) {
                    // Invalid class name
                    continue;
                }
                ClassMatcherFactory factory = FACTORIES.get(kind + ':' + variant);
                if (null == factory) {
                    // Invalid full kind
                    continue;
                }
                matchers.add(factory.create(target));
            }
        } finally {
            try { reader.close(); } catch (IOException ex) {}
        }
        return matchers.isEmpty() ? ClassMatchers.MATCH_NONE : ClassMatchers.whenAny(matchers);
    }
    
    static interface ClassMatcherFactory {
        ClassMatcher create(String className);
    }
    
    private static final Map<String, ClassMatcherFactory> FACTORIES;
    static {
        Map<String, ClassMatcherFactory> map = new HashMap<String, ClassMatcherFileParser.ClassMatcherFactory>();
        
        map.put("class:name", new ClassMatcherFactory() {
            @Override
            public ClassMatcher create(String className) {
                return ClassMatchers.byClassName(className, false);
            }
        });
        map.put("class:name-part", new ClassMatcherFactory() {
            @Override
            public ClassMatcher create(String className) {
                return ClassMatchers.byClassName(className, true);
            }
        });
        map.put("class:name-pattern", new ClassMatcherFactory() {
            @Override
            public ClassMatcher create(String classNamePattern) {
                return ClassMatchers.byClassNamePattern(classNamePattern);
            }
        });       
        
        map.put("extends-class:name", new ClassMatcherFactory() {
            @Override
            public ClassMatcher create(String superClassName) {
                return ClassMatchers.bySuperClassName(superClassName, false);
            }
        });
        map.put("extends-class:name-part", new ClassMatcherFactory() {
            @Override
            public ClassMatcher create(String superClassName) {
                return ClassMatchers.bySuperClassName(superClassName, true);
            }
        });
        map.put("extends-class:name-pattern", new ClassMatcherFactory() {
            @Override
            public ClassMatcher create(String superClassNamePattern) {
                return ClassMatchers.bySuperClassNamePattern(superClassNamePattern);
            }
        });  
        
        map.put("implements-interface:name", new ClassMatcherFactory() {
            @Override
            public ClassMatcher create(String interfaceName) {
                return ClassMatchers.byInterfaceName(interfaceName, false);
            }
        });
        map.put("implements-interface:name-part", new ClassMatcherFactory() {
            @Override
            public ClassMatcher create(String interfaceName) {
                return ClassMatchers.byInterfaceName(interfaceName, true);
            }
        });
        map.put("implements-interface:name-pattern", new ClassMatcherFactory() {
            @Override
            public ClassMatcher create(String interfaceNamePattern) {
                return ClassMatchers.byInterfaceNamePattern(interfaceNamePattern);
            }
        }); 
        
        FACTORIES = Collections.unmodifiableMap(map);
    }
}
