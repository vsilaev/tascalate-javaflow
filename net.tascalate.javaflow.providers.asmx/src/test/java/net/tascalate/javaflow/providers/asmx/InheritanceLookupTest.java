package net.tascalate.javaflow.providers.asmx;

import static org.junit.Assert.assertTrue;

import org.apache.commons.javaflow.providers.asmx.InheritanceLookup;
import org.apache.commons.javaflow.spi.ClasspathResourceLoader;
import org.junit.Test;

public class InheritanceLookupTest {

    @Test
    public void testInheritance() {
        InheritanceLookup lookup = new InheritanceLookup(new ClasspathResourceLoader(ClassLoader.getSystemClassLoader()));
        String sAbstractCollection = lookup.getCommonSuperClass("java/util/ArrayList", "java/util/HashSet");
        assertTrue("java/util/AbstractCollection".equals(sAbstractCollection));
        
        String sIterable1 = lookup.getCommonSuperClass("java/util/List", "java/lang/Iterable");
        assertTrue("java/lang/Iterable".equals(sIterable1));
        
        String sIterable2 = lookup.getCommonSuperClass("java/util/LinkedList", "java/lang/Iterable");
        assertTrue("java/lang/Iterable".equals(sIterable2));

    }
    
}
