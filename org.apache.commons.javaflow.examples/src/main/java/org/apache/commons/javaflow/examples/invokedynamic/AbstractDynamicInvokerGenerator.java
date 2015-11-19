package org.apache.commons.javaflow.examples.invokedynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

public abstract class AbstractDynamicInvokerGenerator {

	byte[] generateInvokeDynamicRunnable(String dynamicInvokerClassName, String dynamicLinkageClassName, String bootstrapMethodName, String targetMethodDescriptor) {

		ClassWriter cw = new ClassWriter(0);
		MethodVisitor mv;

		cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, dynamicInvokerClassName, null, "java/lang/Object", new String[]{"java/lang/Runnable"});
		cw.visitAnnotation("Lorg/apache/commons/javaflow/api/Continuable;", false);
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
			mv.visitAnnotation("Lorg/apache/commons/javaflow/api/Continuable;", false);
			mv.visitCode();
			
			Handle bootstrap = new Handle(
				H_INVOKESTATIC, dynamicLinkageClassName, bootstrapMethodName,
				DYNAMIC_BOOTSTRAP_METHOD_TYPE.toMethodDescriptorString()
			);
			
			int maxStackSize = addMethodParameters(mv);
			mv.visitInvokeDynamicInsn("runCalculation", targetMethodDescriptor, bootstrap);
			mv.visitInsn(RETURN);
			mv.visitMaxs(maxStackSize, 2);
			mv.visitEnd();
		}
		cw.visitEnd();

		return cw.toByteArray();
	}
	
	final Class<?> defineClass(ClassLoader classLoader, String internalClassName, byte[] bytes) {
		try {
			return (Class<?>)DEFINE_CLASS.invokeExact(classLoader, internalClassName.replace('/', '.'), bytes, 0, bytes.length, (ProtectionDomain)null);
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	protected abstract int addMethodParameters(MethodVisitor mv);

	final private static MethodType DYNAMIC_BOOTSTRAP_METHOD_TYPE = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class,
			MethodType.class);
	
	final private static MethodHandle DEFINE_CLASS;
	static {
		try {
			Method m = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
			m.setAccessible(true);
			DEFINE_CLASS = MethodHandles.lookup().unreflect(m);
		} catch (NoSuchMethodException | IllegalAccessException ex) {
			throw new RuntimeException(ex);
		}
	}
}