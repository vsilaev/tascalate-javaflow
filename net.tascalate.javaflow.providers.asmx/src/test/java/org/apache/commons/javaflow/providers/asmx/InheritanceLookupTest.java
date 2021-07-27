package org.apache.commons.javaflow.providers.asmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;

import org.apache.commons.javaflow.spi.ClasspathResourceLoader;
import org.junit.Before;
import org.junit.Test;

public class InheritanceLookupTest {

    ClassHierarchy lookup;
    
    @Before
    public void setup() {
        lookup = new ClassHierarchy(new ClasspathResourceLoader(ClassLoader.getSystemClassLoader()));
    }
   

    @Test
    public void testCache() throws IOException {
        ClassHierarchy.TypeInfo info1 = lookup.getTypeInfo("java/util/LinkedList");
        ClassHierarchy.TypeInfo info2 = lookup.getTypeInfo("java/util/LinkedList");
        assertEquals(info1, info2);
    }

    @Test
    public void testCommonSuperInterface() throws IOException {
        assertEquals("java/util/Collection", lookup.getCommonSuperClass("java/util/List", "java/util/Set"));
    }
    
    @Test
    public void testCommonSuperClass() throws IOException {
        assertEquals("java/util/AbstractCollection", lookup.getCommonSuperClass("java/util/LinkedList", "java/util/HashSet"));
    }
    
    @Test
    public void testCommonSuperClassSymmetrical() throws IOException {
        assertEquals(
            lookup.getCommonSuperClass("java/util/LinkedList", "java/util/List"),
            lookup.getCommonSuperClass("java/util/List", "java/util/LinkedList")            
        );
    }

    @Test
    public void testCommonSuperInterfaceRecursive() throws IOException {
        assertEquals("java/util/Queue", lookup.getCommonSuperClass("org/apache/commons/javaflow/providers/asmx/InheritanceLookupTest$TestList", "java/util/Queue"));
    }
    
    
    @Test
    public void testTypeInfo() throws IOException {
        ClassHierarchy.TypeInfo info1 = lookup.getTypeInfo("java/util/LinkedList");
        assertTrue(info1.isSubclassOf(lookup.getTypeInfo("java/lang/Object")));
        assertTrue(info1.isSubclassOf(lookup.getTypeInfo("java/util/AbstractCollection")));
        assertTrue(info1.isSubclassOf(lookup.getTypeInfo("java/util/Queue")));
        
        ClassHierarchy.TypeInfo info2 = lookup.getTypeInfo("java/util/LinkedHashSet");
        ClassHierarchy.TypeInfo info3 = lookup.getTypeInfo("org/apache/commons/javaflow/providers/asmx/InheritanceLookupTest$TestList");
        ClassHierarchy.TypeInfo info4 = lookup.getTypeInfo("java/util/TreeMap");
        
        System.out.println(info1.flattenHierarchy());
        System.out.println(info2.flattenHierarchy());
        System.out.println(info3.flattenHierarchy());
        System.out.println(info4.flattenHierarchy());
        
        System.out.println(lookup.getCommonSuperClass("java/util/LinkedList", "java/util/LinkedHashSet"));
        System.out.println(lookup.getCommonSuperClass("java/util/List", "java/util/Set"));
    }
    
    static class TestList<T> extends LinkedList<T> implements Externalizable, Closeable {

        @Override
        public void close() {
        }

        @Override
        public void writeExternal(ObjectOutput out) {
        }

        @Override
        public void readExternal(ObjectInput in) {
        }
        
    }
    
}
