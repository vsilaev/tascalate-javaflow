package org.apache.commons.javaflow.instrumentation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.javaflow.spi.ClasspathResourceLoader;

public class ExtendedClasspathResourceLoader extends ClasspathResourceLoader {
	final private static ThreadLocal<Map<String, byte[]>> IN_MEMORY_RESOURCES = new ThreadLocal<Map<String,byte[]>>();
	
	ExtendedClasspathResourceLoader(final ClassLoader classLoader) {
		super(classLoader);
	}
	
	public static void runWithInMemoryResources(final Runnable block, final Map<String, byte[]> inMemoryResources) {
		runWithInMemoryResources(
			new Callable<Void>() {
				public Void call() {
					block.run();
					return null;
				}
			}, 
			inMemoryResources
		);
	}
	
	public static <V> V runWithInMemoryResources(final Callable<V> block, final Map<String, byte[]> inMemoryResources) {
		final Map<String, byte[]> resources = new HashMap<String, byte[]>(inMemoryResources);
		
		final Map<String, byte[]> previous = IN_MEMORY_RESOURCES.get();
		if (null != previous) {
			// Merge with previous ones for recursive calls
			resources.putAll(previous);
		}
		IN_MEMORY_RESOURCES.set(resources);
		
		try {
			return block.call();
		} catch (final RuntimeException ex) {
			throw ex;
		} catch (final Error ex) {
			throw ex;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			IN_MEMORY_RESOURCES.set(previous);
		}
		
	}

	@Override
	public InputStream getResourceAsStream(String name) throws IOException {
		final Map<String, byte[]> inMemoryResources = IN_MEMORY_RESOURCES.get();
		if (null != inMemoryResources) {
			final byte[] bytecode = inMemoryResources.get(name);
			if (null != bytecode)
				return new ByteArrayInputStream(bytecode);
		}
		
		return super.getResourceAsStream(name);
	}
}
