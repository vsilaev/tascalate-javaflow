/**
 * ﻿Original work: copyright 1999-2004 The Apache Software Foundation
 * (http://www.apache.org/)
 *
 * This project is based on the work licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Modified work: copyright 2013-2021 Valery Silaev (http://vsilaev.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.javaflow.tools.jar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.javaflow.spi.ResourceTransformer;
import org.apache.commons.javaflow.spi.AbstractResourceTransformer;
import org.apache.commons.javaflow.spi.ClasspathResourceLoader;
import org.apache.commons.javaflow.spi.FastByteArrayOutputStream;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;

public final class RewritingUtils {

    private final static Logger log = LoggerFactory.getLogger(RewritingUtils.class);

    public interface Matcher {
        boolean isMatching(String name);
    }
    
    private static Matcher MATCH_ALL = new Matcher() {
        public boolean isMatching(String pName) {
            return true;
        }
    };
    
    /*
     * @todo multiple transformers
     */
    public static boolean rewriteClassFile(File input,
                                           ResourceTransformer transformer,
                                           File output) throws IOException {

        byte[] original = toByteArray(input);
        byte[] transformed = transformer.transform(original);
        if (transformed != original /*Exact equality means not transformed*/ || !output.equals(input)) {
            FileOutputStream os = new FileOutputStream(output);
            try {
                os.write(transformed);
            } finally {
                os.close();
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean rewriteJar(JarInputStream input,
                                     ResourceTransformer transformer,
                                     JarOutputStream output) throws IOException {
        return rewriteJar(input, transformer, output, MATCH_ALL);
    }

    public static boolean rewriteJar(JarInputStream input,
                                     ResourceTransformer transformer,
                                     JarOutputStream output,
                                     Matcher pMatcher) throws IOException {

        boolean changed = false;

        while(true) {
            JarEntry entry = input.getNextJarEntry();

            if (entry == null) {
                break;
            }

            if (entry.isDirectory()) {
                output.putNextEntry(new JarEntry(entry));
                continue;
            }

            String name = entry.getName();

            output.putNextEntry(new JarEntry(name));

            if (name.endsWith(".class")) {
                if (pMatcher.isMatching(name)) {

                    if (log.isDebugEnabled()) {
                        log.debug("transforming " + name);
                    }

                    byte[] original = toByteArray(input);
                    byte[] transformed = transformer.transform(original);

                    output.write(transformed);

                    changed |= transformed.length != original.length;

                    continue;
                }
            } else if (name.endsWith(".jar") || 
                       name.endsWith(".ear") || 
                       name.endsWith(".zip") || 
                       name.endsWith(".war")) {

                changed |= rewriteJar(
                    new JarInputStream(input),
                    transformer,
                    new JarOutputStream(output),
                    pMatcher
                );

                continue;
            }

            int length = copy(input,output);

            log.debug("copied " + name + "(" + length + ")");
        }

        input.close();
        output.close();

        return changed;
    }

    public static byte[] toByteArray(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            return toByteArray(in);
        } finally {
            in.close();
        }
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        copy(in,baos);
        return baos.unsafeBytes();
    }

    /**
     * Copies the entire {@link InputStream} to the given {@link OutputStream}.
     *
     * @return
     *      the number of bytes copied.
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        int total = 0;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
            total += n;
        }
        return total;
    }


    public static void main(String[] args) throws FileNotFoundException, IOException {
        ResourceTransformationFactory factory = createTransformerFactoryInstance();
        for (int i=0; i<args.length; i+=2) {
            System.out.println("rewriting " + args[i]);
            
            ResourceTransformer transformer = createTransformer(new URL[]{new File(args[i]).toURI().toURL()}, factory);
            try {
                RewritingUtils.rewriteJar(
                    new JarInputStream(new FileInputStream(args[i])),
                    transformer,
                    new JarOutputStream(new FileOutputStream(args[i+1]))
                );
            } finally {
                transformer.release();
            }
        }

        System.out.println("done");
        
    }
    
    public static ResourceTransformer createTransformer(URL[] extraURL) {
        return createTransformer(extraURL, createTransformerFactoryInstance());
    }

    
    public static ResourceTransformer createTransformer(URL[] extraURL, TransformerType type) {
       return createTransformer(extraURL, createTransformerFactoryInstance(type));
    }
    
    public static ResourceTransformer createTransformer(URL[] extraURL, ResourceTransformationFactory factory) {
        final URLClassLoader classLoader = new URLClassLoader(extraURL, safeParentClassLoader());
        
        final ResourceTransformer transformerDelegate = factory.createTransformer(
            new ClasspathResourceLoader(classLoader)
        );
        
        return new AbstractResourceTransformer() {
            // Need a hard-reference from transformer
            // Otherwise during builds this class loader is evicted!!!
            @SuppressWarnings("unused")
            private final Object hardReference = classLoader;
            
            public byte[] transform(byte[] original, Collection<String> retransformClasses) {
                byte[] transformed = transformerDelegate.transform(original, retransformClasses);
                return null != transformed ? transformed : original;
            }
            
            public void release() {
                transformerDelegate.release();
            }
        };
    }
    
    private static ClassLoader safeParentClassLoader() {
        ClassLoader ownClassLoader = RewritingUtils.class.getClassLoader();
        return null == ownClassLoader ? ClassLoader.getSystemClassLoader() : ownClassLoader;
    }
    
    public static ResourceTransformationFactory createTransformerFactoryInstance() {
        return createTransformerFactoryInstance(null);
    }
    
    public static ResourceTransformationFactory createTransformerFactoryInstance(TransformerType transformerType) {
        Class<? extends ResourceTransformationFactory> transformerFactoryClass;
        if (null == transformerType) {
            transformerFactoryClass = getDefaultResourceTransformerFactoryClass();
        } else {
            try {
                transformerFactoryClass = transformerType.implementaion();
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
        try {
            return transformerFactoryClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static Class<? extends ResourceTransformationFactory> getDefaultResourceTransformerFactoryClass() {
        for (TransformerType transformerType : TransformerType.values()) {
            try {
                return transformerType.implementaion();
            } catch (ClassNotFoundException ex) {
                System.err.println(ex);
            }
        }
        throw new RuntimeException("No bytecode transformation class is found for JavaFlow bytecode modifications");
    }

    public static enum TransformerType {
        ASMX("org.apache.commons.javaflow.providers.core.ContinuableClassTransformationFactory"),
        @Deprecated
        ASM5("org.apache.commons.javaflow.providers.asm5.Asm5ResourceTransformationFactory"),
        @Deprecated
        ASM4("org.apache.commons.javaflow.providers.asm4.Asm4ResourceTransformationFactory"),
        @Deprecated
        ASM3("org.apache.commons.javaflow.providers.asm3.Asm3ResourceTransformationFactory"),
        @Deprecated
        BCEL("org.apache.commons.javaflow.providers.bcel.BcelResourceTransformationFactory");

        private String implementation;

        private TransformerType(String implementation) {
            this.implementation = implementation;
        }

        Class<? extends ResourceTransformationFactory> implementaion() throws ClassNotFoundException { 
            @SuppressWarnings("unchecked")
            Class<? extends ResourceTransformationFactory> c = (Class<? extends ResourceTransformationFactory>) 
            Class.forName(implementation);
            return c;
       }
    }
}
