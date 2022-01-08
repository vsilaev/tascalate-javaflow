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
package org.apache.commons.javaflow.providers.asm5;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class OfflineClassWriter extends ClassWriter {
    private final ClassHierarchy classHierarchy;
    
    public OfflineClassWriter(ClassHierarchy classHierarchy, int flags) {
        super(flags);
        this.classHierarchy = classHierarchy;
    }
    
    public OfflineClassWriter(ClassHierarchy classHierarchy, ClassReader reader, int flags) {
        super(reader, flags);
        this.classHierarchy = classHierarchy;
    }
    
    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        return classHierarchy.getCommonSuperClass(type1, type2);
    }
}
