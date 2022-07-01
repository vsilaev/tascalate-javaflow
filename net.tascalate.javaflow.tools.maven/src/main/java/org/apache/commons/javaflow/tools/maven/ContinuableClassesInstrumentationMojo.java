/**
 * ﻿Copyright 2013-2022 Valery Silaev (http://vsilaev.com)
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.apache.commons.javaflow.spi.ResourceTransformer;
import org.apache.commons.javaflow.tools.jar.RewritingUtils;

/**
 * Maven plugin that will apply Continuation class transformations on compiled
 * classes (bytecode instrumentation).
 * <p>
 * Example plugin configuration :
 * </p>
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
@Mojo(name = "javaflow-enhance",
      threadSafe = true, 
      defaultPhase = LifecyclePhase.PROCESS_CLASSES, 
      requiresDependencyResolution = ResolutionScope.TEST /* ALL DEPENDENCIES */)
public class ContinuableClassesInstrumentationMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", property = "javaflow.enhancer.project", required = true, readonly = true)
    private MavenProject project;

    /** Skips all processing performed by this goal. */
    @Parameter(defaultValue = "false", property = "javaflow.enhancer.skip", required = false)
    private boolean skip;

    @Parameter(defaultValue = "true", property = "javaflow.enhancer.includeTestClasses", required = true)
    /** Whether or not to include test classes to be processed by enhancer. */
    private Boolean includeTestClasses;

    /**
     * Allows to customize the build directory of the project, used for both finding
     * classes to transform and outputing them once transformed. By default, equals
     * to maven's project output directory. Path must be either absolute or relative
     * to project base dir.
     */
    @Parameter(property = "javaflow.enhancer.buildDir", required = false)
    private String buildDir;

    /**
     * Allows to customize the build directory of the tests of the project, used for
     * both finding classes to transform and outputing them once transformed. By
     * default, equals to maven's project test output directory. Path must be either
     * absolute or relative to project base dir.
     */
    @Parameter(property = "javaflow.enhancer.testBuildDir", required = false)
    private String testBuildDir;

    @Component
    private MojoExecution execution;
    
    public void execute() throws MojoExecutionException {
        final Log log = getLog();
        if (skip) {
            log.info("Skipping executing.");
            return;
        }

        try {
            File mainInputDirectory = buildDir == null 
                ? new File(project.getBuild().getOutputDirectory())
                : computeDir(buildDir);
            
            if (mainInputDirectory.exists()) {
                // Use runtime instead of compile - runtime contains non less than compile
                transformFiles(mainInputDirectory, project.getRuntimeClasspathElements()); 
            } else {
                log.warn("No main build output directory available, skipping enhancing main classes");
            }

            if (includeTestClasses) {
                File testInputDirectory = testBuildDir == null
                    ? new File(project.getBuild().getTestOutputDirectory())
                    : computeDir(testBuildDir);

                if (testInputDirectory.exists()) {
                    transformFiles(testInputDirectory, project.getTestClasspathElements()); 
                } else if ("process-test-classes".equals(execution.getLifecyclePhase())) {
                    log.warn("No test build output directory available, skipping enhancing test classes");
                }
            }
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
    
    private void transformFiles(File inputDirectory, List<String> classPathEntries) throws IOException {
        Log log = getLog();
        List<URL> classPath = new ArrayList<URL>();
        for (String classPathEntry : classPathEntries) {
            classPath.add(resolveUrl(new File(classPathEntry)));
        }
        classPath.add(resolveUrl(inputDirectory));

        ResourceTransformer dirTransformer = RewritingUtils.createTransformer(
            classPath.toArray(new URL[] {})
        );
        
        try {
            long now = System.currentTimeMillis();

            for (File source : RecursiveFilesIterator.scanClassFiles(inputDirectory)) {
                if (source.lastModified() <= now) {
                    log.debug("Applying continuations support: " + source);
                    boolean rewritten = RewritingUtils.rewriteClassFile(source, dirTransformer, source);
                    if (rewritten) {
                        log.info("Rewritten continuation-enabled class file: " + source);
                    }
                }
            }
        } finally {
            dirTransformer.release();
        }
    }

    private File computeDir(String dir) {
        File dirFile = new File(dir);
        if (dirFile.isAbsolute()) {
            return dirFile;
        } else {
            return new File(project.getBasedir(), buildDir).getAbsoluteFile();
        }
    }

    private URL resolveUrl(File resource) {
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