package org.apache.commons.javaflow.providers.asm3;

import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;
import org.apache.commons.javaflow.spi.ResourceTransformer;

public class Asm3ResourceTransformationFactory implements ResourceTransformationFactory {

    public ResourceTransformer createTransformer(final ContinuableClassInfoResolver cciResolver) {
        return new Asm3ClassTransformer(cciResolver);
    }

    public ContinuableClassInfoResolver createResolver(final ResourceLoader resourceLoader) {
        return new Asm3ContinuableClassInfoResolver(resourceLoader);
    }

}
