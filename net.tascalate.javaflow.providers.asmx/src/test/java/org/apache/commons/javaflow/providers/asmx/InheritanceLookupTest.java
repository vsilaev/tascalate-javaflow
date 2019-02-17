package org.apache.commons.javaflow.providers.asmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.javaflow.providers.asmx.ClassHierarchy;
import org.apache.commons.javaflow.spi.ClasspathResourceLoader;
import org.junit.Test;

public class InheritanceLookupTest {

    
    @Test
    public void testTypeInfo() throws IOException {
        ClassHierarchy lookup = new ClassHierarchy(new ClasspathResourceLoader(ClassLoader.getSystemClassLoader()));
        
        ClassHierarchy.TypeInfo info1 = lookup.getTypeInfo("java/util/LinkedList");
        assertTrue(info1.isSubclassOf(lookup.getTypeInfo("java/lang/Object")));
        assertTrue(info1.isSubclassOf(lookup.getTypeInfo("java/util/AbstractCollection")));
        assertTrue(info1.isSubclassOf(lookup.getTypeInfo("java/util/Queue")));
        
        assertEquals("java/util/AbstractCollection", lookup.getCommonSuperClass("java/util/LinkedList", "java/util/HashSet"));
        assertEquals("java/util/Collection", lookup.getCommonSuperClass("java/util/List", "java/util/Set"));
    }
    
}
