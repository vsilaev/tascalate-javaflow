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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public abstract class AbstractResourceTransformer implements ResourceTransformer {
    
    public byte[] transform(byte[] original) {
        return transform(original, Collections.<String>emptySet());
    }
    
    public byte[] transform(byte[] original, String retransformClass) {
        return transform(original, Collections.<String>singleton(retransformClass));
    }
    
    public byte[] transform(byte[] original, String... retransformClasses) {
        return transform(
            original, null == retransformClasses || retransformClasses.length == 0 ?
                      Collections.<String>emptySet() : 
                      new HashSet<String>(Arrays.asList(retransformClasses))
        );
    }
}
