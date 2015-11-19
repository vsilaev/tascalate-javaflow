package org.apache.commons.javaflow.spi;

import java.io.IOException;

public interface ContinuableClassInfoResolver {
	abstract public ContinuableClassInfo forget(final String className); 
	abstract public ContinuableClassInfo resolve(final String className) throws IOException;
	abstract public ContinuableClassInfo resolve(final String className, final byte[] classBytes);
}
