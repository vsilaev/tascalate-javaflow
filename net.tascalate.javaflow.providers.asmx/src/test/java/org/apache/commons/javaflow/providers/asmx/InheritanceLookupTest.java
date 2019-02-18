package org.apache.commons.javaflow.providers.asmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.LinkedList;

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
        
        ClassHierarchy.TypeInfo info2 = lookup.getTypeInfo("org/apache/commons/javaflow/providers/asmx/InheritanceLookupTest$TestList");
        ClassHierarchy.TypeInfo info3 = lookup.getTypeInfo("java/util/Set");
        
        System.out.println(info1.flattenHierarchy());
        System.out.println(info2.flattenHierarchy());
        System.out.println(info3.flattenHierarchy());
        
        assertEquals("java/util/AbstractCollection", lookup.getCommonSuperClass("java/util/LinkedList", "java/util/HashSet"));
        assertEquals("java/util/Collection", lookup.getCommonSuperClass("java/util/List", "java/util/Set"));
        
        System.out.println(lookup.getCommonSuperClass("java/util/LinkedList", "java/util/HashSet"));
        System.out.println(lookup.getCommonSuperClass("java/util/List", "java/util/Set"));
    }
    
    @SuppressWarnings("serial")
    static class TestList<T> extends LinkedList<T> {}
    
}
