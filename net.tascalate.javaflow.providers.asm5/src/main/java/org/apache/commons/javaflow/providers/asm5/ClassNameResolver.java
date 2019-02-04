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
package org.apache.commons.javaflow.providers.asm5;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.javaflow.spi.StopException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

public class ClassNameResolver {
    public static class Result {
        public final String className;
        public final byte[] classfileBuffer;

        Result(String className, byte[] classfileBuffer) {
            this.className = className;
            this.classfileBuffer = classfileBuffer;
        }

        public Map<String, byte[]> asResource() {
            return Collections.singletonMap(className + ".class", classfileBuffer);
        }        
    }

    public static Result resolveClassName(String className, Class<?> classBeingRedefined, byte[] classfileBuffer) {
        String resolvedClassName = className != null ? className :
            classBeingRedefined != null ? classBeingRedefined.getName().replace('.', '/') : null;

        final String[] classNameFromBytes = {null}; 
        if (null == resolvedClassName) {
            try {
                ClassReader cv = new ClassReader(classfileBuffer);
                cv.accept(new ClassVisitor(AsmVersion.CURRENT) {
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        classNameFromBytes[0] = name;
                        throw StopException.INSTANCE;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            } catch (StopException exIgnore) {

            }
            resolvedClassName = classNameFromBytes[0];
        }
        return new Result(resolvedClassName, classfileBuffer);
    }
}