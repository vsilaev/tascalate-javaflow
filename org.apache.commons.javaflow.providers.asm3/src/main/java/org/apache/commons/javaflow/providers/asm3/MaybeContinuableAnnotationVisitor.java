package org.apache.commons.javaflow.providers.asm3;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.Opcodes;

public class MaybeContinuableAnnotationVisitor extends ClassAdapter {
    private final Asm3ContinuableClassInfoResolver environment; 
    private boolean classContinuableAnnotationFound = false;
    private boolean isAnnotation = false;

    public MaybeContinuableAnnotationVisitor(final Asm3ContinuableClassInfoResolver environment) {
        super(MaybeContinuableClassVisitor.NOP);
        this.environment = environment;
    }

    public void visit( int version, int access, String name, String signature, String superName, String[] interfaces ) {
        isAnnotation = (access & Opcodes.ACC_ANNOTATION) > 0;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String description, boolean visible) {
        if (isAnnotation && !classContinuableAnnotationFound) {
            classContinuableAnnotationFound = environment.isContinuableAnnotation(description);
        }
        return null;
    }

    boolean isContinuable() { 
        return classContinuableAnnotationFound && isAnnotation; 
    }
}

