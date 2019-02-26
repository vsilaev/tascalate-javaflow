package org.apache.commons.javaflow.providers.asmx;

import org.apache.commons.javaflow.spi.ResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;

import net.tascalate.asmx.ClassReader;

public abstract class AbstractResourceTransformationFactory implements ResourceTransformationFactory {
    
    public abstract ContinuableClassInfoResolver createResolver(ResourceLoader resourceLoader);
    
    public String readClassName(byte[] classBytes) {
        return new ClassReader(classBytes).getClassName();
    }
}
