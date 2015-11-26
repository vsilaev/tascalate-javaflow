package org.apache.commons.javaflow.examples.invokedynamic;

import java.net.URL;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.providers.asm5.Asm5ResourceTransformationFactory;
import org.apache.commons.javaflow.utils.ContinuationClassLoader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class DynamicInvokerExample {

	public static void main(String[] args) throws Exception {
		Class<Runnable> dynamicClass = generateDynamicInvokerClass("org/apache/commons/javaflow/examples/invokedynamic/SimpleDynamicInvoker");
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

		@SuppressWarnings("resource")
		ContinuationClassLoader delegateClassLoader = new ContinuationClassLoader(
			new URL[]{}, DynamicInvokerExample.class.getClassLoader(),
			new Asm5ResourceTransformationFactory()
		);

		@SuppressWarnings("unchecked")
		Class<Runnable> dynamicClass = (Class<Runnable>)delegateClassLoader.defineClassFromData(dynamicClassBytes, dynamicInvokerClassName);
		return dynamicClass;
	}

}