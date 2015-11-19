package org.apache.commons.javaflow.examples.invokedynamic;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.javaflow.api.Continuation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class DynamicInvokerExample {

	public static void main(String[] args) throws Exception {

		Class<Runnable> dynamicClass = generateDynamicInvokerClass("demo/SimpleDynamicInvoker");
		Runnable demo = dynamicClass.newInstance();

		for (Continuation cc = Continuation.startWith(demo); null != cc; cc = Continuation.continueWith(cc)) {
			System.out.println("Interrupted " + cc.value());
		}
	}
	
	private static Class<Runnable> generateDynamicInvokerClass(final String dynamicInvokerClassName) {
		AbstractDynamicInvokerGenerator generator = new AbstractDynamicInvokerGenerator() {
			@Override
			protected int addMethodParameters(final MethodVisitor mv) {
				return 0;
			}			
		};
		
		byte[] dynamicClassBytes = generator.generateInvokeDynamicRunnable(
			dynamicInvokerClassName, 
			Type.getType(SimpleDynamicLinkage.class).getInternalName(), 
			"bootstrapDynamic", "()V"
		);

		// Hack to make class bytes available during agent instrumentation
		ClassLoader delegateClassLoader = new URLClassLoader(new URL[]{}, DynamicInvokerExample.class.getClassLoader()) {
			@Override
			public InputStream getResourceAsStream(String name) {
				if (name.equals(dynamicInvokerClassName + ".class")) {
					return new ByteArrayInputStream(dynamicClassBytes);
				}
				return super.getResourceAsStream(name);
			}
			
		};
		
		@SuppressWarnings("unchecked")
		Class<Runnable> dynamicClass = (Class<Runnable>)generator.defineClass(delegateClassLoader, dynamicInvokerClassName, dynamicClassBytes);
		return dynamicClass;
	}

}