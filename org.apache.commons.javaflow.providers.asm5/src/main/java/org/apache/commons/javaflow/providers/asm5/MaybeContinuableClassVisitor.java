package org.apache.commons.javaflow.providers.asm5;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.javaflow.core.Continuable;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class MaybeContinuableClassVisitor extends ClassVisitor {
	private final Asm5ContinuableClassInfoResolver environment; 
	private boolean classContinuableAnnotationFound = false;
	private boolean classContinuatedMarkerFound = false;
	private String superclass;
	private String[] superinterfaces;
	private String outerClassName;
	private String outerClassMethodName;
	private String outerClassMethodDesc;
	
	Set<String> continuableMethods = new HashSet<String>();
	
	private boolean isInterface = false;
	private boolean isAnnotation = false;
	private boolean isLambda = false;

	public MaybeContinuableClassVisitor(final Asm5ContinuableClassInfoResolver environment) {
		super(Opcodes.ASM5);
		this.environment = environment;
	}

	final private static Pattern LAMBDA_CLASS_NAME = Pattern.compile("^(.*)\\$\\$Lambda\\$\\d+$");
	
	public void visit( int version, int access, String name, String signature, String superName, String[] interfaces ) {
		isInterface = (access & Opcodes.ACC_INTERFACE) > 0;
		isAnnotation = (access & Opcodes.ACC_ANNOTATION) > 0;
		
		if ((access & Opcodes.ACC_SUPER) != 0 && (access & Opcodes.ACC_FINAL) != 0 && (access & Opcodes.ACC_SYNTHETIC) != 0) {
			final Matcher matcher = LAMBDA_CLASS_NAME.matcher(name);
			if (matcher.matches()) {
				try {
					final boolean isOwnerContinuable = environment.resolve(matcher.group(1)) != null; 
					isLambda = isOwnerContinuable;
				} catch (final IOException exIgnore) {
					// Should never happen here -- when lambda is being defined 
					// the declaring class is already loaded
					
				}
			}
		}

		superclass = superName;
		superinterfaces = interfaces;
		
		if (!isInterface && null != interfaces) for (final String interfaceInternalName : interfaces) {
			if (CONTINUABLE_MARKER_INTERFACE_NAME.equals(interfaceInternalName)) {
				classContinuatedMarkerFound = true;
				break;
			}
		}
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(final String description, boolean visible) {
		if (!classContinuableAnnotationFound) {
			classContinuableAnnotationFound = environment.isContinuableAnnotation(description);
		}
		return null;
	}
	
	@Override
	public void visitOuterClass(String owner, String name, String desc) { 
		outerClassName = owner;
		outerClassMethodName = name;
		outerClassMethodDesc = desc;
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
		visitInheritanceChain();
		// If desugared lambda method in outer class
		if ( (access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC)) != 0 && name.startsWith("lambda$") ){
			continuableMethods.add(name + desc);
			return null;
		}
		if (isLambda && (access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0) {
			// Non-private non-static methods in lambda are SAM implementation + additional bridges
			continuableMethods.add(name + desc);
			return null;
		}
		// Otherwise check annotations
		return new MethodVisitor(Opcodes.ASM5) {
			
			private boolean methodContinuableAnnotationFound = false;
			
			@Override
			public AnnotationVisitor visitAnnotation(final String description, boolean visible) {
				if (!methodContinuableAnnotationFound) {
					methodContinuableAnnotationFound = environment.isContinuableAnnotation(description);
				}
				return null;
			}
			
			@Override 
			public void visitEnd() {
				if (methodContinuableAnnotationFound) {
					continuableMethods.add(name + desc);
				}
			}
			
		};
	}
	
	@Override
	public void visitEnd() {
		visitInheritanceChain();
		checkOuterClass();
	}
	
	private boolean inheritanceChainVisited = false;
	private void visitInheritanceChain() {
		if (!inheritanceChainVisited) {
			inheritanceChainVisited = true;
			if (!isAnnotation) {
				if (null != superclass && !OBJECT_CLASS_INTERNAL_NAME.equals(superclass)) 
					visitParentClass(superclass);
				if (null != superinterfaces) for (final String superinterface : superinterfaces)
					visitParentClass(superinterface);

			}
		}
	}

	private void visitParentClass(final String classInternalName) {
		final ContinuableClassInfoInternal parent = resolve(classInternalName);
		if (null != parent) {
			continuableMethods.addAll(parent.continuableMethods());
		}
	}
	
	private void checkOuterClass() {
		if (outerClassName != null && outerClassMethodName != null) {
			if (!continuableMethods.isEmpty()) {
				final ContinuableClassInfoInternal outer = resolve(outerClassName);
				if (null != outer && outer.isContinuableMethod(0, outerClassMethodName, outerClassMethodDesc, null)) {
					;
				}
			}
		}		
	}
	
	private ContinuableClassInfoInternal resolve(final String classInternalName) {
		try {
			return (ContinuableClassInfoInternal)environment.resolve(classInternalName);
		} catch (final IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	boolean isContinuable() { 
		return 
			(classContinuableAnnotationFound && isAnnotation) || 
			!continuableMethods.isEmpty(); 
	}
	
	// Java8 allows implementation in interfaces
	boolean isProcessed() { 
		return /*isInterface || */classContinuatedMarkerFound; 
	}
	
	private final static String CONTINUABLE_MARKER_INTERFACE_NAME = Type.getInternalName(Continuable.class); 
	private final static String OBJECT_CLASS_INTERNAL_NAME = Type.getInternalName(Object.class);
}
