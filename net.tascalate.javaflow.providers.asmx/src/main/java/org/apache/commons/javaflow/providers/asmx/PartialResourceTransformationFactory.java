package org.apache.commons.javaflow.providers.asmx;

import java.io.IOException;

import org.apache.commons.javaflow.spi.Cache;
import org.apache.commons.javaflow.spi.ClassMatcher;
import org.apache.commons.javaflow.spi.ClassMatchers;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformer;
import org.apache.commons.javaflow.spi.VetoableResourceLoader;

public class PartialResourceTransformationFactory extends AbstractResourceTransformationFactory {

    public ResourceTransformer createTransformer(ResourceLoader resourceLoader) {
        throw new UnsupportedOperationException();
    }
    
    public ContinuableClassInfoResolver createResolver(ResourceLoader resourceLoader) {
        return new IContinuableClassInfoResolver(
            resourceLoader,
            CACHED_SHARED.get(resourceLoader)
        );
    }

    static SharedContinuableClassInfos getCached(ResourceLoader resourceLoader) {
        return CACHED_SHARED.get(resourceLoader);
    }
    
    static ClassMatcher createVeto(ResourceLoader resourceLoader) {
        if (resourceLoader instanceof VetoableResourceLoader) {
            try {
                return ((VetoableResourceLoader)resourceLoader).createVeto();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return ClassMatchers.MATCH_NONE;
        }
    }
    
    private static final Cache<ResourceLoader, SharedContinuableClassInfos> CACHED_SHARED = 
        new Cache<ResourceLoader, SharedContinuableClassInfos>() {
            @Override
            protected SharedContinuableClassInfos createValue(ResourceLoader loader) {
                return new SharedContinuableClassInfos(
                    new ClassHierarchy(loader), createVeto(loader)
                );
            }
        };    
    
}