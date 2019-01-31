/**
 * ﻿Copyright 2013-2017 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;

import java.security.ProtectionDomain;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import org.apache.commons.javaflow.providers.asmx.AsmxResourceTransformationFactory;
import org.apache.commons.javaflow.providers.asmx.ClassNameResolver;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.ExtendedClasspathResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;
import org.apache.commons.javaflow.spi.ResourceTransformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class JavaFlowClassTransformer implements ClassFileTransformer {
	final private static Log log = LogFactory.getLog(JavaFlowClassTransformer.class);
	
	final private ResourceTransformationFactory resourceTransformationFactory = new AsmxResourceTransformationFactory(); 


	//@Override
	public byte[] transform(ClassLoader classLoader, final String className,
			final Class<?> classBeingRedefined,
			final ProtectionDomain protectionDomain,
			final byte[] classfileBuffer) throws IllegalClassFormatException {

	    if (skipClassByName(className)) {
	        if (log.isDebugEnabled()) {
	            log.debug("Ignoring class by name (looks like Java std. class): " + className);
	        }
	        return null;
	    }
	    
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
			} catch (ClassCircularityError ex) {
			    if (log.isWarnEnabled()) {
			        log.warn("Ignoring class circularity error: " + ex.getMessage());
			    }
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
	
	static boolean skipClassByName(String className) {
	    return null != className && (
	           className.startsWith("java/") ||
	           className.startsWith("javax/") ||
	           className.startsWith("sun/") ||
	           className.startsWith("com/sun/") ||
	           className.startsWith("oracle/") ||
	           className.startsWith("com/oracle/") ||
	           className.startsWith("ibm/") ||
	           className.startsWith("com/ibm/")
	           );
	}

	final private static Map<ClassLoader, ContinuableClassInfoResolver> classLoader2resolver = new WeakHashMap<ClassLoader, ContinuableClassInfoResolver>();
}
	