/**
 * ﻿Original work: copyright 1999-2004 The Apache Software Foundation
 * (http://www.apache.org/)
 *
 * This project is based on the work licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Modified work: copyright 2013-2019 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.tools.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.javaflow.spi.ClasspathResourceLoader;
import org.apache.commons.javaflow.spi.FastByteArrayOutputStream;
import org.apache.commons.javaflow.spi.MorphingResourceLoader;
import org.apache.commons.javaflow.spi.ResourceTransformationFactory;
import org.apache.commons.javaflow.spi.ResourceTransformer;

/**
 * {@link URLClassLoader} with bytecode instrumentation for javaflow.
 *
 * <p>
 * This class loader is useful where the application can set up multiple class
 * loaders (such as in a container environment,
 * <a href="http://classworlds.codehaus.org/">ClassWorlds</a>, or
 * <a href="http://forehead.werken.com/">Forehead</a>) and when you can isolate
 * the continuation-enabled portion of your application into a separate jar
 * file.
 */
public class ContinuableClassLoader extends ClassLoader {

    private final static Logger log = LoggerFactory.getLogger(ContinuableClassLoader.class);

    public static abstract class AbstractBuilder<L extends ContinuableClassLoader, B extends AbstractBuilder<L, B>> {
        protected final ResourceTransformationFactory transformationFactory;

        protected ClassLoader parent;
        protected boolean parentFirst = true;
        protected boolean isolated = false;
        protected List<String> systemPackages = new ArrayList<String>();
        protected List<String> loaderPackages = new ArrayList<String>();

        /**
         * Creates a classloader builder.
         *
         * @param transformationFactory
         *            This factory is used to create necessary
         *            resolver/transformer to perform the byte-code enhancement.
         *            May not be null.
         */
        protected AbstractBuilder(ResourceTransformationFactory transformationFactory) {
            this.transformationFactory = transformationFactory;
        }

        abstract public L create();

        /**
         * Set parent class loader
         * 
         * @param parent
         *            the parent classloader to which unsatisfied loading
         *            attempts are delegated. May be <code>null</code>, in which
         *            case the {@link ClassLoader#getSystemClassLoader() system
         *            classloader} is used as the parent.
         * 
         * @return this builder
         */
        public B parent(ClassLoader parent) {
            this.parent = parent;
            return self();
        }

        /**
         * Control whether class lookup is delegated to the parent loader first
         * or after this loader. Use with extreme caution. Setting this to false
         * violates the class loader hierarchy and can lead to Linkage errors
         *
         * @param parentFirst
         *            if true, delegate initial class search to the parent
         *            classloader.
         * 
         * @return this builder
         */
        public B parentFirst(boolean parentFirst) {
            this.parentFirst = parentFirst;
            return self();
        }

        /**
         * Sets whether this classloader should run in isolated mode. In
         * isolated mode, classes not found on the given classpath will not be
         * referred to the parent class loader but will cause a
         * ClassNotFoundException.
         *
         * @param isolated
         *            Whether or not this classloader should run in isolated
         *            mode.
         * 
         * @return this builder
         */
        public B isolated(boolean isolated) {
            this.isolated = isolated;
            return self();
        }

        /**
         * Adds a package root to the list of packages which must be loaded on
         * the parent loader.
         *
         * All subpackages are also included.
         *
         * @param packageRoot
         *            The root of all packages to be included. Should not be
         *            <code>null</code>.
         * 
         * @return this builder
         */
        public B addSystemPackageRoot(String packageRoot) {
            systemPackages.add(packageRoot);
            return self();
        }

        /**
         * Adds a package root to the list of packages which must be loaded
         * using this loader.
         *
         * All subpackages are also included.
         *
         * @param packageRoot
         *            The root of all packages to be included. Should not be
         *            <code>null</code>.
         * 
         * @return this builder
         */
        public B addLoaderPackageRoot(String packageRoot) {
            loaderPackages.add(packageRoot);
            return self();
        }

        @SuppressWarnings("unchecked")
        final private B self() {
            return (B) this;
        }
    }

    public static class Builder extends AbstractBuilder<ContinuableClassLoader, Builder> {
        /**
         * Creates a classloader builder.
         *
         * @param transformationFactory
         *            This factory is used to create necessary
         *            resolver/transformer to perform the byte-code enhancement.
         *            May not be null.
         */
        public Builder(ResourceTransformationFactory transformationFactory) {
            super(transformationFactory);
        }

        @Override
        public ContinuableClassLoader create() {
            return new ContinuableClassLoader(
                parent, transformationFactory, 
                parentFirst, isolated, systemPackages, loaderPackages
            );
        }
    }

    protected final ResourceTransformationFactory transforationFactory;
    protected final MorphingResourceLoader resourceLoader;

    /**
     * Indicates whether the parent class loader should be consulted before
     * trying to load with this class loader.
     */
    protected final boolean parentFirst;

    /**
     * These are the package roots that are to be loaded by the parent class
     * loader regardless of whether the parent class loader is being searched
     * first or not.
     */
    protected final List<String> systemPackages;

    /**
     * These are the package roots that are to be loaded by this class loader
     * regardless of whether the parent class loader is being searched first or
     * not.
     */
    protected final List<String> loaderPackages;

    /**
     * Whether or not this classloader will ignore the base classloader if it
     * can't find a class.
     *
     */
    protected final boolean isolated;

    /* The context to be used when loading classes and resources */
    protected final AccessControlContext acc;
    
    protected final ClassLoader platformClassLoader;

    /**
     * Creates a classloader to define classes dynamically.
     *
     * @param parent
     *            The parent classloader to which unsatisfied loading attempts
     *            are delegated. May be <code>null</code>, in which case the
     *            {@link ClassLoader#getSystemClassLoader() system classloader}
     *            is used as the parent.
     * @param transformationFactory
     *            This factory is used to create necessary resolver/transformer
     *            to perform the byte-code enhancement. May not be null.
     */
    public ContinuableClassLoader(ClassLoader parent, ResourceTransformationFactory transformationFactory) {
        this(parent, transformationFactory, true, false, null, null);
    }

    /**
     * Creates a classloader by using settings given.
     *
     * @param parent
     *            The parent classloader to which unsatisfied loading attempts
     *            are delegated. May be <code>null</code>, in which case the
     *            {@link ClassLoader#getSystemClassLoader() system classloader}
     *            is used as the parent.
     * @param transformationFactory
     *            This factory is used to create necessary resolver/transformer
     *            to perform the byte-code enhancement. May not be null.
     * @param parentFirst
     *            If true, delegate initial class search to the parent
     *            classloader.
     * @param isolated
     *            Whether or not this classloader should run in isolated
     *            mode. 
     * @param systemPackages
     *            The package roots that are to be loaded by the parent class
     *            loader regardless of whether the parent class loader is being
     *            searched first or not.
     * @param loaderPackages
     *            The package roots that are to be loaded by this class loader
     *            regardless of whether the parent class loader is being
     *            searched first or not.
     */
    public ContinuableClassLoader(ClassLoader parent, 
                                  ResourceTransformationFactory transformationFactory,
                                  boolean parentFirst, 
                                  boolean isolated, 
                                  List<String> systemPackages, 
                                  List<String> loaderPackages) {
        
        super(fixNullParent(parent));
        
        if (transformationFactory == null)
            throw new IllegalArgumentException();
        
        this.transforationFactory = transformationFactory;
        this.resourceLoader = new MorphingResourceLoader(
            new ClasspathResourceLoader(this)
        );
        
        this.acc = AccessController.getContext();
        this.parentFirst = parentFirst;
        this.isolated = isolated;
        this.systemPackages = dotEndingPackageNames(systemPackages);
        this.loaderPackages = dotEndingPackageNames(loaderPackages);
        
        this.platformClassLoader = ClassLoader.getSystemClassLoader().getParent();
    }

    public static Builder builder(ResourceTransformationFactory transformationFactory) {
        return new Builder(transformationFactory);
    }

    /**
     * Loads a class through this class loader even if that class is available
     * on the parent classpath.
     *
     * This ensures that any classes which are loaded by the returned class will
     * use this classloader.
     *
     * @param className
     *            The name of the class to be loaded. Must not be
     *            <code>null</code>.
     *
     * @return the required Class object
     *
     * @exception ClassNotFoundException
     *                if the requested class does not exist on this loader's
     *                classpath.
     */
    public Class<?> forceLoadClass(String className) throws ClassNotFoundException {
        log.debug("force loading " + className);

        Class<?> theClass = findLoadedClass(className);

        if (theClass == null) {
            theClass = findClass(className);
        }

        return theClass;
    }

    /**
     * Tests whether or not the parent classloader should be checked for a
     * resource before this one. If the resource matches both the "use parent
     * classloader first" and the "use this classloader first" lists, the latter
     * takes priority.
     *
     * @param resourceName
     *            The name of the resource to check. Must not be
     *            <code>null</code>.
     *
     * @return whether or not the parent classloader should be checked for a
     *         resource before this one is.
     */
    private boolean isParentFirst(String resourceName) {
        // default to the global setting and then see
        // if this class belongs to a package which has been
        // designated to use a specific loader first
        // (this one or the parent one)

        if (resourceName.startsWith("org.apache.commons.javaflow.tools.runtime.") || 
            resourceName.startsWith("org.apache.commons.javaflow.spi.")) {
            return true;
        }
        
//        if (resourceName.startsWith("org.apache.commons.javaflow.") && !resourceName.startsWith("org.apache.commons.javaflow.examples.")) {
//            return true;
//        }
        
        boolean useParentFirst = parentFirst;

        for (String packageName : systemPackages) {
            if (resourceName.startsWith(packageName)) {
                useParentFirst = true;
                break;
            }
        }

        for (String packageName : loaderPackages) {
            if (resourceName.startsWith(packageName )) {
                useParentFirst = false;
                break;
            }
        }

        return useParentFirst;
    }

    /**
     * Loads a class with this class loader.
     *
     * This class attempts to load the class in an order determined by whether
     * or not the class matches the system/loader package lists, with the loader
     * package list taking priority. If the classloader is in isolated mode,
     * failure to load the class in this loader will result in a
     * ClassNotFoundException.
     *
     * @param className
     *            The name of the class to be loaded. Must not be
     *            <code>null</code>.
     * @param resolve
     *            <code>true</code> if all classes upon which this class depends
     *            are to be loaded.
     *
     * @return the required Class object
     *
     * @exception ClassNotFoundException
     *                if the requested class does not exist on the system
     *                classpath (when not in isolated mode) or this loader's
     *                classpath.
     */
    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        // 'sync' is needed - otherwise 2 threads can load the same class
        // twice, resulting in LinkageError: duplicated class definition.
        // findLoadedClass avoids that, but without sync it won't work.
        Object lock = safeGetClassLoadingLock(className);
        synchronized (lock) {
            Class<?> theClass = findLoadedClass(className);
            if (theClass != null) {
                return theClass;
            }
    
            ClassLoader parentClassLoader = getParent();
            if (null == parentClassLoader) {
                parentClassLoader = platformClassLoader;
            }
            
            if (isParentFirst(className)) {
                try {
                    theClass = parentClassLoader.loadClass(className);
                    log.debug("Class " + className + " loaded from parent loader " + "(parentFirst)");
                } catch (ClassNotFoundException cnfe) {
                    theClass = findClass(className);
                    log.debug("Class " + className + " loaded from own loader " + "(parentFirst)");
                }
            } else {
                // It's an error to load anything from platform
                if (null != platformClassLoader) {
                    try {
                        theClass = platformClassLoader.loadClass(className); 
                        log.debug("Class " + className + " is platform class");
                    } catch (ClassNotFoundException ex) {
                        log.debug("Class " + className + " is not visible to platform, will try to load on own loader");
                    }
                }
                // Not found among platform classes
                if (null == theClass) {
                    try {
                        theClass = findClass(className);
                        log.debug("Class " + className + " loaded from own loader");
                    } catch (ClassNotFoundException cnfe) {
                        if (isolated) {
                            throw cnfe;
                        }
                        theClass = parentClassLoader.loadClass(className);
                        log.debug("Class " + className + " loaded from parent loader");
                    }
                }
            }
    
            if (resolve) {
                resolveClass(theClass);
            }
    
            return theClass;
        }
    }
    
    public Class<?> defineClassFromData(byte[] classData, String className) {
        return defineClassFromData(classData, className, getClass().getProtectionDomain());
    }
    
    /**
     * Define a class given its bytes
     *
     * @param classData
     *            the bytecode data for the class
     * @param className
     *            the name of the class
     * @param protectionDomain
     *            the protection domain of the class
     *
     * @return the Class instance created from the given data
     */
    public Class<?> defineClassFromData(final byte[] classData, 
                                        final String className, 
                                        final ProtectionDomain protectionDomain) {
        return AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
            public Class<?> run() {
                String internalClassName = transforationFactory.readClassName(classData);
                String classNameFromData = internalClassName.replace('/', '.');
                
                if (null != className && !className.equals(classNameFromData)) {
                    throw new IllegalArgumentException(
                        "Supplied class name (" + className + ") " +
                        "does not much class name in data (" + classNameFromData + ")."
                    );
                }
                // define a package if necessary.
                int i = classNameFromData.lastIndexOf('.');
                if (i > 0) {
                    final String packageName = classNameFromData.substring(0, i);
                    @SuppressWarnings("all")
                    final Package pkg = getPackage(packageName);
                    if (pkg == null) {
                        definePackage(packageName, null, null, null, null, null, null, null);
                    }
                }

                ResourceTransformer transformer = transforationFactory.createTransformer(
                    resourceLoader.withReplacement(internalClassName, classData)
                );
                
                byte[] transfomred;
                try {
                    transfomred = transformer.transform(classData, classNameFromData);                        
                } finally {
                    transformer.release();
                }

                if (null == transfomred)
                    transfomred = classData;

                return defineClass(null, transfomred, 0, transfomred.length, protectionDomain);
            }
        }, acc);
    }

    /**
     * Reads a class definition from a stream.
     *
     * @param stream
     *            The stream from which the class is to be read. Must not be
     *            <code>null</code>.
     * @param className
     *            The name of the class in the stream. Must not be
     *            <code>null</code>.
     *
     * @return the Class object read from the stream.
     *
     * @exception IOException
     *                if there is a problem reading the class from the stream.
     * @exception SecurityException
     *                if there is a security problem while reading the class
     *                from the stream.
     */
    private Class<?> getClassFromStream(InputStream stream, String className) throws IOException, SecurityException {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream(BUFFER_SIZE);
        try {

            int bytesRead;
            byte[] buffer = new byte[BUFFER_SIZE];

            while ((bytesRead = stream.read(buffer, 0, BUFFER_SIZE)) > 0) {
                baos.write(buffer, 0, bytesRead);
            }
            
            byte[] classData = baos.unsafeBytes();
            return defineClassFromData(classData, className);

        } finally {
            baos.close();
        }
    }

    /**
     * Searches for and load a class on the classpath of this class loader.
     *
     * @param name
     *            The name of the class to be loaded. Must not be
     *            <code>null</code>.
     *
     * @return the required Class object
     *
     * @exception ClassNotFoundException
     *                if the requested class does not exist on this loader's
     *                classpath.
     */
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        log.debug("Finding class " + name);

        // locate the class file
        String classFileName = name.replace('.', '/') + ".class";

        InputStream stream = getResourceAsStream(classFileName);
        if (stream == null)
            throw new ClassNotFoundException(name);

        try {
            return getClassFromStream(stream, name);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Finds the resource with the given name. A resource is some data (images,
     * audio, text, etc) that can be accessed by class code in a way that is
     * independent of the location of the code.
     *
     * @param name
     *            The name of the resource for which a stream is required. Must
     *            not be <code>null</code>.
     * @return a URL for reading the resource, or <code>null</code> if the
     *         resource could not be found or the caller doesn't have adequate
     *         privileges to get the resource.
     */
    @Override
    public URL getResource(String name) {
        // we need to search the components of the path to see if
        // we can find the class we want.
        if (isParentFirst(name)) {
            return super.getResource(name);
        }

        // try this class loader first, then parent
        URL url = findResource(name);
        if (url == null) {
            url = getParent().getResource(name);
        }
        return url;
    }
    
    private Object safeGetClassLoadingLock(String name) {
        if (null == GET_CLASS_LOADING_LOCK) {
            return this;
        }
        try {
            return GET_CLASS_LOADING_LOCK.invoke(this, name);
        } catch (IllegalAccessException ex) {
            // Should not happen
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            // Should not happen
            throw ex;
        } catch (InvocationTargetException ex) {
            // Should not happen
            throw new RuntimeException(ex);
        }
    }

    private static final int BUFFER_SIZE = 4096;
    
    private static Method getClassLoaderMethodOrNull(String name, Class<?>... args) {
        try {
            Method method = ClassLoader.class.getDeclaredMethod(name, args);
            try {
                // Need for Java 8 and will success
                method.setAccessible(true);
            } catch (Exception ex) {
                // Ignore in Java 9
                // - in this context (getting protected method of a superclass)
                // method may be called anyway 
            }
            return method;
        } catch (NoSuchMethodException ex) {
            // OK, JDK version is less then 1.7
            return null;
        } catch (SecurityException ex) {
            // Should be available, if method exists
            throw ex;
        }
    }
    
    private static final Method GET_CLASS_LOADING_LOCK = 
        getClassLoaderMethodOrNull("getClassLoadingLock", String.class);
    
    static { 
        Method registerAsParallelCapable = getClassLoaderMethodOrNull("registerAsParallelCapable");
        if (null != registerAsParallelCapable) {
            try {
                registerAsParallelCapable.invoke(null);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static ClassLoader fixNullParent(ClassLoader classLoader) {
        if (classLoader != null) {
            return classLoader;
        } else {
            return getSystemClassLoader();
        }
    }

    private static List<String> dotEndingPackageNames(List<String> source) {
        if (null == source) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (String s : source) {
            if (s != null && s.length() > 0) {
                s += '.';
            }
            result.add(s);
        }
        return result;
    }
}
