package org.apache.commons.javaflow.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.apache.commons.javaflow.spi.ResourceTransformer;
import org.apache.commons.javaflow.tools.RewritingUtils;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;

/**
 * Ant task that enhances class files with javaflow instrumentation.
 */
public class AntRewriteTask extends MatchingTask {

	private RewritingUtils.TransformerType transformerType;

	private File dstDir;
	private File srcDir;
	private Path compileClasspath;

    /**
     * Directory to which the transformed files will be written.
     * This can be the same as the source directory.
     * 
     * @param pFile destination directory
     */
    public void setDestdir(final File pFile) {
        dstDir = pFile;
    }
	
    /**
     * Directory from which the input files are read.
     * This and the inherited MatchingTask forms an implicit
     * FileSet.
     * 
     * @param pFile source directory
     */
    public void setSrcDir(final File pFile) {
        srcDir = pFile;
        fileset.setDir(srcDir);
    }

    /**
     * Sets the transformer to use.
     *
     * <p>
     * This option is unpublished, because in a long run we'll
     * likely to just focus on one transformer and get rid
     * of the other (and this option will be removed then.)
     *
     * @param name
     *      "ASM5". Case insensitive.
     */
    public void setMode(String name) {
    	try {
    		RewritingUtils.TransformerType.valueOf(name.toUpperCase());
    	} catch (final RuntimeException ex) {
    		throw new BuildException("Unrecognized mode: " + name);
    	}
    }
    
    
    /**
     * Set the classpath to be used for this compilation.
     *
     * @param classpath an Ant Path object containing the compilation classpath.
     */
    public void setClasspath(final Path classpath) {
        if (compileClasspath == null) {
            compileClasspath = classpath;
        } else {
            compileClasspath.append(classpath);
        }
    }

    /**
     * Gets the classpath to be used for this compilation.
     * @return the class path
     */
    public Path getClasspath() {
        return compileClasspath;
    }

    /**
     * Adds a path to the classpath.
     * @return a class path to be configured
     */
    public Path createClasspath() {
        if (compileClasspath == null) {
            compileClasspath = new Path(getProject());
        }
        return compileClasspath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     * @param r a reference to a classpath
     */
    public void setClasspathRef(final Reference r) {
        createClasspath().setRefid(r);
    }
	    
    /**
     * Check that all required attributes have been set and nothing
     * silly has been entered.
     *
     * @since Ant 1.5
     */
    protected void checkParameters() throws BuildException {
        checkDir(srcDir,"srcDir");
        checkDir(dstDir,"dstDir");
    }

    private void checkDir(final File pDir, final String pDescription) {
        if (pDir == null) {
            throw new BuildException("no " + pDescription + " directory is specified", getLocation());
        }
        if (!pDir.exists()) {
            throw new BuildException(pDescription + " directory \"" + pDir + "\" does not exist", getLocation());
        }
        if (!pDir.isDirectory()) {
            throw new BuildException(pDescription + " directory \"" + pDir + "\" is not a directory", getLocation());
        }
    }
	
    public void execute() throws BuildException {
        final DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
        final String[] fileNames = ds.getIncludedFiles();
        try {
        	createClasspath();

        	final List<URL> classPath = new ArrayList<URL>();
        	for (final Iterator<Resource> i = compileClasspath.iterator(); i.hasNext();) {
        		final FileResource resource = (FileResource)i.next();
        		classPath.add( resource.getFile().toURI().toURL() );
        	}

        	final List<URL> classPathByDir = new ArrayList<URL>(classPath);
        	classPathByDir.add(srcDir.toURI().toURL());
        	
            final ResourceTransformer dirTransformer = RewritingUtils.createTransformer(
                	classPathByDir.toArray(new URL[]{}),
                	transformerType
                );
        	
            for (final String fileName : fileNames) {
                final File source = new File(srcDir, fileName);
                final File destination = new File(dstDir, fileName);
                
                if (!destination.getParentFile().exists()) {
                    log("Creating dir: " + destination.getParentFile(), Project.MSG_VERBOSE);
                    destination.getParentFile().mkdirs();
                }

                if (source.lastModified() < destination.lastModified()) {
                    log("Omitting " + source + " as " + destination + " is up to date", Project.MSG_VERBOSE);
                    continue;
                }
                
                if (fileName.endsWith(".class")) {
                	log("Rewriting " + source + " to " + destination, Project.MSG_VERBOSE);
                    // System.out.println("Rewriting " + source);

                    RewritingUtils.rewriteClassFile( source, dirTransformer, destination );
                }

                if (fileName.endsWith(".jar")
                    || fileName.endsWith(".ear")
                    || fileName.endsWith(".zip")
                    || fileName.endsWith(".war")) {
	
                    log("Rewriting " + source + " to " + destination, Project.MSG_VERBOSE);

                	final List<URL> classPathByJar = new ArrayList<URL>(classPath);
                	classPathByJar.add(source.toURI().toURL());
                    final ResourceTransformer jarTransformer = RewritingUtils.createTransformer(
                    	classPathByJar.toArray(new URL[]{}), 
                    	transformerType
                    );
                    
                    RewritingUtils.rewriteJar(
                    	new JarInputStream(new FileInputStream(source)),
                    	jarTransformer,
                    	new JarOutputStream(new FileOutputStream(destination))
                    );
                    
                }
            }
        } catch (IOException e) {
	          throw new BuildException(e);
        }
    }
}