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
package org.apache.commons.javaflow.instrumentation.cdi;

import java.util.Collection;
import java.util.List;

import org.apache.commons.javaflow.providers.asmx.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.AbstractResourceTransformer;
import org.apache.commons.javaflow.spi.StopException;

import net.tascalate.asmx.ClassReader;
import net.tascalate.asmx.ClassVisitor;
import net.tascalate.asmx.ClassWriter;
import net.tascalate.asmx.plus.ClassHierarchy;
import net.tascalate.asmx.plus.OfflineClassWriter;

class ContinuableProxyTransformer extends AbstractResourceTransformer {

    private final ClassHierarchy classHierarchy;
    private final ContinuableClassInfoResolver cciResolver;
    private final List<ProxyType> proxyTypes;

    ContinuableProxyTransformer(ClassHierarchy classHierarchy, 
                                ContinuableClassInfoResolver cciResolver,
                                List<ProxyType> proxyTypes) {
        
        this.classHierarchy = classHierarchy;
        this.cciResolver = cciResolver;
        this.proxyTypes = proxyTypes;
    }

    public byte[] transform(byte[] original, Collection<String> retransformClasses) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new OfflineClassWriter(classHierarchy, reader, ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ContinuableProxyAdapter(writer, classHierarchy, cciResolver, proxyTypes);
        cciResolver.reset(retransformClasses);
        try {
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        } catch (StopException ex) {
            // Preliminary stop visiting non-continuable-proxy class
            return null;
        }

        byte[] bytecode = writer.toByteArray();
        return bytecode;
    }
    
    public void release() {
        cciResolver.release();
    }
}