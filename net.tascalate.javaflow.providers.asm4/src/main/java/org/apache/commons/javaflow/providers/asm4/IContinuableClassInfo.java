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
package org.apache.commons.javaflow.providers.asm4;

import java.util.Set;

import org.apache.commons.javaflow.spi.ContinuableClassInfo;

class IContinuableClassInfo implements ContinuableClassInfo {
    private boolean processed;
    private final Set<String> methods;

    IContinuableClassInfo(boolean defaultProcessed, Set<String> methods) {
        this.processed = defaultProcessed;
        this.methods = methods;
    }
    
    public boolean isContinuableMethod(int access, String name, String desc, String signature) {
        return methods.contains(name + desc);
    }

    boolean isClassProcessed() {
        return processed;
    }

    void markClassProcessed() {
        processed = true;
    }

    Set<String> continuableMethods() {
        return methods;
    }

}