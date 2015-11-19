package org.apache.commons.javaflow.spi;

public interface ContinuableClassInfo {
	abstract public boolean isContinuableMethod(int access, String name, String desc, String signature);
	abstract public boolean isClassProcessed();
	abstract public void markClassProcessed();
}
