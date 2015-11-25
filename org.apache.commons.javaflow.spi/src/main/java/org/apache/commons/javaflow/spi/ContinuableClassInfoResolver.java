package org.apache.commons.javaflow.spi;

import java.io.IOException;

public interface ContinuableClassInfoResolver {
	abstract public ContinuableClassInfo forget(String className); 
	abstract public ContinuableClassInfo resolve(String className) throws IOException;
	abstract public ContinuableClassInfo resolve(String className, byte[] classBytes);
	abstract public boolean isContinuableAnnotation(String annotationClassDescriptor);
	abstract public ResourceLoader resourceLoader();
}
