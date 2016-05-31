package org.apache.commons.javaflow.spi;

public interface ResourceTransformationFactory {
	abstract public ResourceTransformer createTransformer(ContinuableClassInfoResolver cciResolver);
	abstract public ContinuableClassInfoResolver createResolver(ResourceLoader resourceLoader);
}
