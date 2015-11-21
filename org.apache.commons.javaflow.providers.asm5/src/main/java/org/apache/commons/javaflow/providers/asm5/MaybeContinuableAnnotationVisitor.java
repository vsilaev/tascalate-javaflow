package org.apache.commons.javaflow.providers.asm5;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class MaybeContinuableAnnotationVisitor extends ClassVisitor {
	private final Asm5ContinuableClassInfoResolver environment; 
	private boolean classContinuableAnnotationFound = false;
	private boolean isAnnotation = false;

	public MaybeContinuableAnnotationVisitor(final Asm5ContinuableClassInfoResolver environment) {
		super(Opcodes.ASM5);
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

