package org.apache.commons.javaflow.spi;

import java.io.IOException;
import java.io.InputStream;

public class ClasspathResourceLoader implements ResourceLoader {

	final private ClassLoader classLoader;
	
	public ClasspathResourceLoader(final ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	
	public InputStream getResourceAsStream(String name) throws IOException {
		final InputStream result = classLoader.getResourceAsStream(name);
		if (null == result) {
			throw new IOException("Unable to find resource " + name);
		}
		return result;
	}
}
