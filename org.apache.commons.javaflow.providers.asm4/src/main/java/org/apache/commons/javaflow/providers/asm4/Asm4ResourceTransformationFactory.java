package org.apache.commons.javaflow.providers.asm4;

import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;
import org.apache.commons.javaflow.spi.ResourceTransformer;

public class Asm4ResourceTransformationFactory implements ResourceTransformationFactory {

    public ResourceTransformer createTransformer(final ContinuableClassInfoResolver cciResolver) {
        return new Asm4ClassTransformer(cciResolver);
    }

    public ContinuableClassInfoResolver createResolver(final ResourceLoader resourceLoader) {
        return new Asm4ContinuableClassInfoResolver(resourceLoader);
    }

}
