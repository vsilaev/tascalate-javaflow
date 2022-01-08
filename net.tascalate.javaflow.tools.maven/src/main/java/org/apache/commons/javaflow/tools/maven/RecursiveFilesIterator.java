/**
 * ﻿Copyright 2013-2021 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.tools.maven;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RecursiveFilesIterator implements Iterator<File> {

    public static final FileFilter ANY_READABLE_FILE = new FileFilter() {
        public boolean accept(File f) {
            return f.exists() && f.isFile() && f.canRead();
        }
    };

    public static final FileFilter CLASS_FILE = new FileFilter() {
        public boolean accept(File f) {
            return ANY_READABLE_FILE.accept(f) && f.getName().endsWith(".class");
        }
    };

    private static final FileFilter DIRECTORY = new FileFilter() {
        public boolean accept(File f) {
            return f.exists() && f.isDirectory() && f.canRead();
        }
    };

    private final File rootDir;
    private final FileFilter fileFilter;

    private boolean usedFiles = false;
    private boolean usedDirs = false;
    private Iterator<File> currentDelegate;
    private Iterator<File> currentDirs;

    public static Iterable<File> scanClassFiles(File rootDir) {
        return scanFiles(rootDir, CLASS_FILE);
    }

    public static Iterable<File> scanFiles(final File rootDir, final FileFilter fileFilter) {
        return new Iterable<File>() {
            public Iterator<File> iterator() {
                return new RecursiveFilesIterator(rootDir, fileFilter);
            }
        };
    }

    public RecursiveFilesIterator(File rootDir, FileFilter fileFilter) {
        if (null == rootDir) {
            throw new IllegalArgumentException("Directory parameter may not be null");
        }
        if (!DIRECTORY.accept(rootDir)) {
            throw new IllegalArgumentException(rootDir + " is not an existing readable directory");
        }
        this.rootDir = rootDir;
        this.fileFilter = fileFilter;
    }

    protected void setupDelegate() {
        if (!usedFiles) {
            usedFiles = true;
            File[] files = rootDir.listFiles(fileFilter);
            if (files != null && files.length > 0) {
                currentDelegate = Arrays.asList(files).iterator();
            } else {
                currentDelegate = Collections.<File>emptySet().iterator();
            }
        }

        if (!currentDelegate.hasNext()) {
            if (!usedDirs) {
                usedDirs = true;
                File[] dirs = rootDir.listFiles(DIRECTORY);
                if (dirs != null && dirs.length > 0) {
                    currentDirs = Arrays.asList(dirs).iterator();
                } else {
                    currentDirs = Collections.<File>emptySet().iterator();
                }
            }

            while (!currentDelegate.hasNext() && currentDirs.hasNext()) {
                currentDelegate = new RecursiveFilesIterator(currentDirs.next(), fileFilter);
            }
        }
    }

    public boolean hasNext() {
        setupDelegate();
        return null != currentDelegate && currentDelegate.hasNext();
    }

    public File next() {
        setupDelegate();
        if (null != currentDelegate) {
            return currentDelegate.next();
        } else {
            throw new NoSuchElementException();
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
