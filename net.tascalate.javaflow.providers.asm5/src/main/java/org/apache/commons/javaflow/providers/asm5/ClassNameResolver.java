package org.apache.commons.javaflow.providers.asm5;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.javaflow.spi.StopException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ClassNameResolver {
    public static class Result {
        final public String className;
        final public byte[] classfileBuffer;

        Result(final String className, final byte[] classfileBuffer) {
            this.className = className;
            this.classfileBuffer = classfileBuffer;
        }

        public Map<String, byte[]> asResource() {
            return Collections.singletonMap(className + ".class", classfileBuffer);
        }        
    }

    public static Result resolveClassName(final String className, final Class<?> classBeingRedefined, final byte[] classfileBuffer) {
        String resolvedClassName = className != null ? className :
            classBeingRedefined != null ? classBeingRedefined.getName().replace('.', '/') : null;

        final String[] classNameFromBytes = {null}; 
        if (null == resolvedClassName) {
            try {
                final ClassReader cv = new ClassReader(classfileBuffer);
                cv.accept(new ClassVisitor(Opcodes.ASM5) {
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        classNameFromBytes[0] = name;
                        throw StopException.INSTANCE;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            } catch (final StopException exIgnore) {

            }
            resolvedClassName = classNameFromBytes[0];
        }
        return new Result(resolvedClassName, classfileBuffer);
    }
}
