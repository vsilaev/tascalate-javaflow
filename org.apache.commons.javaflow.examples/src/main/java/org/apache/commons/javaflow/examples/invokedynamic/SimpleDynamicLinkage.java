package org.apache.commons.javaflow.examples.invokedynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.api.Continuation;

@continuable 
public class SimpleDynamicLinkage {
	
	@continuable 
	static void sayHello() {
		System.out.println("There we go!");
		for (char c = 'A'; c < 'G'; c++)
			Continuation.suspend("Data" + c);
		System.out.println("CallSite continuation finished");
	}

	public static CallSite bootstrapDynamic(final MethodHandles.Lookup caller, final String name, final MethodType type) throws NoSuchMethodException, IllegalAccessException {
		final MethodHandles.Lookup lookup = MethodHandles.lookup();
		final Class<?> thisClass = lookup.lookupClass(); // (who am I?)
		final MethodHandle sayHello = lookup.findStatic(thisClass, "sayHello", MethodType.methodType(void.class));
		return new ConstantCallSite(sayHello.asType(type));
	}

}