package org.apache.commons.javaflow.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;

import java.security.ProtectionDomain;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import org.apache.commons.javaflow.providers.asm5.Asm5ResourceTransformationFactory;
import org.apache.commons.javaflow.providers.asm5.ClassNameResolver;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.ExtendedClasspathResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;
import org.apache.commons.javaflow.spi.ResourceTransformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class JavaFlowClassTransformer implements ClassFileTransformer {
	final private static Log log = LogFactory.getLog(JavaFlowClassTransformer.class);
	
	final private ResourceTransformationFactory resourceTransformationFactory = new Asm5ResourceTransformationFactory(); 


	//@Override
	public byte[] transform(ClassLoader classLoader, final String className,
			final Class<?> classBeingRedefined,
			final ProtectionDomain protectionDomain,
			final byte[] classfileBuffer) throws IllegalClassFormatException {

		classLoader = getSafeClassLoader(classLoader);
		final ContinuableClassInfoResolver resolver = getCachedResolver(classLoader);

		synchronized (resolver) {
			final ClassNameResolver.Result currentTarget = ClassNameResolver.resolveClassName(className, classBeingRedefined, classfileBuffer);
			try {
				// Execute with current class as extra resource (in-memory)
				// Mandatory for Java8 lambdas and alike
				return ExtendedClasspathResourceLoader.runWithInMemoryResources(
					new Callable<byte[]>() {
						public byte[] call() {
							resolver.forget(currentTarget.className);
							final ResourceTransformer transformer = resourceTransformationFactory.createTransformer(resolver);
							return transformer.transform(classfileBuffer);
						}
					}, 
					currentTarget.asResource()
				);
			} catch (final RuntimeException ex) {
				log.error(ex);
				return null;
			} catch (final Error ex) {
				log.error(ex);
				throw ex;
			}
		}
	}

	
	protected ClassLoader getSafeClassLoader(final ClassLoader classLoader) {
		return null != classLoader ? classLoader : ClassLoader.getSystemClassLoader(); 
	}
	
	protected ContinuableClassInfoResolver getCachedResolver(final ClassLoader classLoader) {
		synchronized (classLoader2resolver) {
			final ContinuableClassInfoResolver cachedResolver = classLoader2resolver.get(classLoader);
			if (null == cachedResolver) {
				final ContinuableClassInfoResolver newResolver = resourceTransformationFactory.createResolver(new ExtendedClasspathResourceLoader(classLoader));
				classLoader2resolver.put(classLoader, newResolver);
				return newResolver;
			} else {
				return cachedResolver;
			}
		}
	}

	final private static Map<ClassLoader, ContinuableClassInfoResolver> classLoader2resolver = new WeakHashMap<ClassLoader, ContinuableClassInfoResolver>();
}
	