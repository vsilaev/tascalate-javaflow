package org.apache.commons.javaflow.tools.maven;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class RecursiveFilesIterator implements Iterator<File> {
	
	final public static FileFilter ANY_READABLE_FILE = new FileFilter() {
		public boolean accept(final File f) {
			return f.exists() && f.isFile() && f.canRead();
		}
	};
	
	final public static FileFilter CLASS_FILE = new FileFilter() {
		public boolean accept(final File f) {
			return ANY_READABLE_FILE.accept(f) && f.getName().endsWith(".class");
		}
	};
	
	final private static FileFilter DIRECTORY = new FileFilter() {
		public boolean accept(final File f) {
			return f.exists() && f.isDirectory() && f.canRead();
		}
	};
	
	final private File rootDir;
	final private FileFilter fileFilter;
	
	private boolean usedFiles = false;
	private boolean usedDirs  = false;
	private Iterator<File> currentDelegate;
	private Iterator<File> currentDirs;
	
	
	public static Iterable<File> scanClassFiles(final File rootDir) {
		return scanFiles(rootDir, CLASS_FILE);
	}
	
	public static Iterable<File> scanFiles(final File rootDir, final FileFilter fileFilter) {
		return new Iterable<File>() {
			public Iterator<File> iterator() {
				return new RecursiveFilesIterator(rootDir, fileFilter);
			}
		};
	}
	
	public RecursiveFilesIterator(final File rootDir, final FileFilter fileFilter) {
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
			final File[] files = rootDir.listFiles(fileFilter);
			if (files != null && files.length > 0) {
				currentDelegate = Arrays.asList(files).iterator();
			} else {
				currentDelegate = Collections.<File>emptySet().iterator();
			}
		}
		
		if (!currentDelegate.hasNext() ) {
			if (!usedDirs) {
				usedDirs = true;
				final File[] dirs = rootDir.listFiles(DIRECTORY);
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
