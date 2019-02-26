package org.apache.commons.javaflow.providers.asmx;

import java.io.IOException;

import org.apache.commons.javaflow.spi.Cache;
import org.apache.commons.javaflow.spi.ClassMatcher;
import org.apache.commons.javaflow.spi.ClassMatchers;
import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;
import org.apache.commons.javaflow.spi.ResourceTransformer;
import org.apache.commons.javaflow.spi.VetoableResourceLoader;

import net.tascalate.asmx.ClassReader;

public abstract class AbstractResourceTransformationFactory implements ResourceTransformationFactory {

    public ResourceTransformer createTransformer(ResourceLoader resourceLoader) {
        SharedContinuableClassInfos cciShared = CACHED_SHARED_CCI.get(resourceLoader);
        return createTransformer(
            resourceLoader, 
            new IContinuableClassInfoResolver(resourceLoader, cciShared),
            // Actualize ClassHierarchy per resource loader
            cciShared.hierarchy().shareWith(resourceLoader)
        );
    }
    
    abstract protected ResourceTransformer createTransformer(ResourceLoader resourceLoader,
                                                             ContinuableClassInfoResolver resolver,
                                                             ClassHierarchy classHierarchy);
    
    public ContinuableClassInfoResolver createResolver(ResourceLoader resourceLoader) {
        return new IContinuableClassInfoResolver(
            resourceLoader,
            CACHED_SHARED_CCI.get(resourceLoader)
        );
    }
    
    public String readClassName(byte[] classBytes) {
        return new ClassReader(classBytes).getClassName();
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
    
    private static final Cache<ResourceLoader, SharedContinuableClassInfos> CACHED_SHARED_CCI = 
        new Cache<ResourceLoader, SharedContinuableClassInfos>() {
            @Override
            protected SharedContinuableClassInfos createValue(ResourceLoader loader) {
                return new SharedContinuableClassInfos(
                    new ClassHierarchy(loader), createVeto(loader)
                );
            }
        };    
}
