/**
 * ﻿Copyright 2013-2021 Valery Silaev (http://vsilaev.com)
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

class ClassMatchStrategyFileParser {
    ClassMatchStrategy parse(URL url) throws IOException {
        List<ClassMatchStrategy> matchers = new ArrayList<ClassMatchStrategy>();
        BufferedReader reader = new LineNumberReader(new InputStreamReader(url.openStream()));
        try {
            String s; 
            int lineIdx = 0;
            while (null != (s = reader.readLine())) {
                if (s.length() == 0 || s.startsWith("#")) {
                    continue;
                }
                int columnPos = s.indexOf(':');
                int equalsPos = s.indexOf('=');
                if (columnPos < 0 || equalsPos < 0) {
                    // Invalid line
                    throw new IOException("Unparsable entry at line #" + lineIdx + 
                                          " (invalid kind/variant) of " + 
                                          url.toExternalForm());
                }
                String kind    = s.substring(0, columnPos).trim();
                String variant = s.substring(columnPos + 1, equalsPos).trim();
                String target  = s.substring(equalsPos + 1).trim();
                if (target.length() == 0) {
                    // Invalid class name
                    throw new IOException("Unparsable entry at line #" + lineIdx + 
                                          " (missing name/name-part/pattern) of " + 
                                          url.toExternalForm());
                }
                StrategyFactory factory = STRATEGY_FACTORIES.get(kind + ':' + variant);
                if (null == factory) {
                    // Invalid full kind
                    throw new IOException("Unknown kind/variant \"" +  (kind + ':' + variant) + 
                                          "\" at line #" + lineIdx + 
                                          url.toExternalForm());
                }
                matchers.add(factory.create(target));
                lineIdx++;
            }
        } finally {
            try { reader.close(); } catch (IOException ex) {}
        }
        return matchers.isEmpty() ? ClassMatchStrategies.MATCH_NONE : ClassMatchStrategies.whenAny(matchers);
    }
    
    static interface StrategyFactory {
        ClassMatchStrategy create(String className);
    }
    
    static enum VariantFactory {
        BY_NAME_FULL() {
            ClassMatchStrategy create(String option) {
                return ClassMatchStrategies.byClassName(option, false);
            }
        },
        BY_NAME_PART() {
            ClassMatchStrategy create(String option) {
                return ClassMatchStrategies.byClassName(option, true);
            }
        },
        BY_NAME_PATTERN() {
            ClassMatchStrategy create(String option) {
                return ClassMatchStrategies.byClassNamePattern(option);
            }
        };        
        
        abstract ClassMatchStrategy create(String option);
    }
    
    static enum KindFactory {
        CLASS() {
            ClassMatchStrategy create(ClassMatchStrategy nested) {
                return nested;
            }
        },
        SUPERCLASS() {
            ClassMatchStrategy create(ClassMatchStrategy nested) {
                return ClassMatchStrategies.bySuperClass(nested);
            }
        },
        INTERFACE() {
            ClassMatchStrategy create(ClassMatchStrategy nested) {
                return ClassMatchStrategies.byInterface(nested);
            }
        };
        
        abstract ClassMatchStrategy create(ClassMatchStrategy nested);
        
        StrategyFactory compose(final VariantFactory nested) {
            return new StrategyFactory() {
                @Override
                public ClassMatchStrategy create(String option) {
                    return KindFactory.this.create(nested.create(option));
                }
            };
        }
    }
    
    private static final Map<String, StrategyFactory> STRATEGY_FACTORIES;
    static {
        Map<String, StrategyFactory> map = new HashMap<String, ClassMatchStrategyFileParser.StrategyFactory>();
        String[] keys = {
            "class:name", "class:name-part", "class:name-pattern",
            "extends-class:name", "extends-class:name-part", "extends-class:name-pattern",
            "implements-interface:name", "implements-interface:name-part", "implements-interface:name-pattern"
        };
        int idx = 0;
        for (KindFactory kindFactory : KindFactory.values()) {
            for (VariantFactory variantFactory : VariantFactory.values()) {
                map.put(keys[idx++], kindFactory.compose(variantFactory));
            }
        }
        STRATEGY_FACTORIES = Collections.unmodifiableMap(map);
    }
}
