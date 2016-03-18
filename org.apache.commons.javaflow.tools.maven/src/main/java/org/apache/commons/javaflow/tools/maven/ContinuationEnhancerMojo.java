package org.apache.commons.javaflow.tools.maven;

import static java.lang.Thread.currentThread;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.javaflow.spi.ResourceTransformer;
import org.apache.commons.javaflow.tools.RewritingUtils;
import org.apache.commons.javaflow.tools.RewritingUtils.TransformerType;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Maven plugin that will apply Continuation
 * class transformations on compiled classes (bytecode instrumentation).
 * <p>Example plugin configuration :</p>
 * 
 * <pre>
 *   &lt;configuration&gt;
 *       &lt;skip&gt;true&lt;/skip&gt;
 *       &lt;includeTestClasses&gt;false&lt;/includeTestClasses&gt;
 *       &lt;buildDir&gt;bin/classes&lt;/buildDir&gt;
 *       &lt;testBuildDir&gt;bin/test-classes&lt;/testBuildDir&gt;
 *   &lt;/configuration&gt;
 * </pre>
 * 
 */
@Mojo(name = "javaflow-enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ContinuationEnhancerMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", property = "javaflow.enhancer.project", required = true, readonly = true)
	private MavenProject project;

	/**Skips all processing performed by this goal.*/
	@Parameter(defaultValue = "false", property = "javaflow.enhancer.skip", required = false)
	private boolean skip;

	@Parameter(defaultValue = "true", property = "javaflow.enhancer.includeTestClasses", required = true)
	/**Whether or not to include test classes to be processed by enhancer.*/
	private Boolean includeTestClasses;

	/** Allows to customize the build directory of the project, used for both finding classes to transform and outputing them once transformed. 
	 * By default, equals to maven's project output directory. Path must be either absolute or relative to project base dir.*/
	@Parameter(property = "javaflow.enhancer.buildDir", required = false)
	private String buildDir;

	/** Allows to customize the build directory of the tests of the project, used for both finding classes to transform and outputing them once transformed. 
	 * By default, equals to maven's project test output directory. Path must be either absolute or relative to project base dir.*/
	@Parameter(property = "javaflow.enhancer.testBuildDir", required = false)
	private String testBuildDir;

	public void execute() throws MojoExecutionException {
		final Log log = getLog();
		if( skip ) {
			log.info("Skipping executing.");
			return;
		}

		final ClassLoader originalContextClassLoader =
			currentThread().getContextClassLoader();

		try {
			final List<URL> classPath = new ArrayList<URL>();

			for (final String runtimeResource : project.getRuntimeClasspathElements()) {
				classPath.add(resolveUrl(new File(runtimeResource)));
			}

			final File inputDirectory = buildDir == null ?
				new File(project.getBuild().getOutputDirectory()) : 
				computeDir(buildDir);

			classPath.add(resolveUrl(inputDirectory));

			loadAdditionalClassPath(classPath);

			final ResourceTransformer dirTransformer = RewritingUtils.createTransformer(
				classPath.toArray(new URL[]{}), TransformerType.ASM5
			);
			
			final long now = System.currentTimeMillis();
			
			for (final File source : RecursiveFilesIterator.scanClassFiles(inputDirectory)) {
				if (source.lastModified() <= now) {
					log.debug("Applying continuations support: " + source);
					final boolean rewritten = RewritingUtils.rewriteClassFile( source, dirTransformer, source );
					if (rewritten) {
						log.info("Rewritten continuation-enabled class file: " + source);
					}
				}
			}
			
			if (includeTestClasses) {
				final File testInputDirectory = testBuildDir == null ? 
					new File(project.getBuild().getTestOutputDirectory()) : 
					computeDir(testBuildDir);
				
				if (testInputDirectory.exists()) {
					for (final File source : RecursiveFilesIterator.scanClassFiles(testInputDirectory)) {
						if (source.lastModified() <= now) {
							log.debug("Applying continuations support: " + source);
							final boolean rewritten = RewritingUtils.rewriteClassFile( source, dirTransformer, source );
							if (rewritten) {
								log.info("Rewritten continuation-enabled class file: " + source);
							}

						}
					}
				}
			}
		}
		catch (final Exception e) {
			getLog().error(e.getMessage(), e);
			throw new MojoExecutionException(e.getMessage(), e);
		}
		finally {
			currentThread().setContextClassLoader(originalContextClassLoader);
		}
	}

	private void loadAdditionalClassPath(final List<URL> classPath) {
		if (classPath.isEmpty()) {
			return;
		}
		final ClassLoader contextClassLoader =
			currentThread().getContextClassLoader();

		final URLClassLoader pluginClassLoader =
			URLClassLoader.newInstance(
				classPath.toArray(new URL[classPath.size()]),
				contextClassLoader);

		currentThread().setContextClassLoader(pluginClassLoader);
	}

    private File computeDir(String dir) {
        File dirFile = new File( dir );
        if( dirFile.isAbsolute() ) {
            return dirFile;
        } else { 
            return new File(project.getBasedir(), buildDir).getAbsoluteFile();
        }
    }


	private URL resolveUrl(final File resource) {
		try {
			return resource.toURI().toURL();
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public boolean isSkip() {
        return skip;
    }
	
	public Boolean getIncludeTestClasses() {
        return includeTestClasses;
    }

	public String getBuildDir() {
        return buildDir;
    }
	
	public String getTestBuildDir() {
        return testBuildDir;
    }

}