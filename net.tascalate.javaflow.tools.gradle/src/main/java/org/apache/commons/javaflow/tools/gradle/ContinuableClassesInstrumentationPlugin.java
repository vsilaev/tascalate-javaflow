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
package org.apache.commons.javaflow.tools.gradle;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;

import org.apache.commons.javaflow.spi.RecursiveFilesIterator;
import org.apache.commons.javaflow.spi.ResourceTransformer;
import org.apache.commons.javaflow.tools.jar.RewritingUtils;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;

/**
 * Gradle plugin that will apply Continuation class transformations on compiled
 * classes (bytecode instrumentation).
 * <p>
 * Example plugin configuration :
 * </p>
 * 
 * <pre>
 * buildscript {
 *     repositories {
 *         mavenCentral() // change this to mavenLocal() if you're testing a local build of this plugin
 *     }
 * 
 *     dependencies {
 *         classpath group: 'net.tascalate.javaflow',  name: 'net.tascalate.javaflow.tools.gradle',  version: 'PUT_CORRECT_VERSION_HERE'
 *     }
 * }
 * 
 * apply plugin: "java"
 * apply plugin: "continuations"
 * 
 * continuations {
 *     // skip = true
 *     // includeTestClasses = false 
 * }
 * 
 * repositories {
 *     mavenCentral()
 * }
 * 
 * dependencies {
 *     compile group: 'net.tascalate.javaflow', name: 'net.tascalate.javaflow.api', version: 'PUT_CORRECT_VERSION_HERE'
 * }
 * </pre>
 * 
 */
public class ContinuableClassesInstrumentationPlugin implements Plugin<Project> {
    
    private Logger log;

    @Override
    public void apply(Project target) {
        ContinuableClassesInstrumentationPluginConfiguration config = new ContinuableClassesInstrumentationPluginConfiguration();
        target.getExtensions().add("continuations", config);
        
        log = target.getLogger();

        Set<Task> compileJavaTasks = target.getTasksByName("compileJava", true);
        for (Task task : compileJavaTasks) {
            addInstrumentActionToTask("main", task, config);
        }

        Set<Task> compileJavaTestTasks = target.getTasksByName("compileTestJava", true);
        for (Task task : compileJavaTestTasks) {
            addInstrumentActionToTask("test", task, config);
        }
    }    
    
    private void addInstrumentActionToTask(final String sourceType, final Task task, final ContinuableClassesInstrumentationPluginConfiguration config) {
        task.doLast(new Action<Task>() {
            @Override
            public void execute(Task arg0) {
                if (config.isSkip()) {
                    log.info("Skipping execution.");
                    return;
                }
                
                if ("test".equals(sourceType) && !config.isIncludeTestClasses()) {
                    log.info("Skipping execution on test classes.");
                    return;
                }

                try {
                    Project project = task.getProject();
                    SourceSetContainer sourceSetsContainer = (SourceSetContainer)project.getProperties().get("sourceSets");
                    SourceSet sourceSet = sourceSetsContainer.getByName(sourceType);
                    if (null != sourceSet) {
                        Set<File> compileClasspath = sourceSet.getCompileClasspath().getFiles();
                        SourceSetOutput output = sourceSet.getOutput();
                        Set<File> classesDirs = output.getClassesDirs().getFiles();
                        instrument(classesDirs, compileClasspath, config);
                    }
                } catch (Exception e) {
                    log.log(LogLevel.ERROR, "Coroutines instrumentation failed", e);
                    throw new IllegalStateException("Coroutines instrumentation failed" , e);
                }
            }
        });
    }
    
    private void instrument(Set<File> classesDirs, Set<File> compileClasspath, ContinuableClassesInstrumentationPluginConfiguration config) {
        try {
            List<File> classpath = new ArrayList<File>();
            log.debug("Getting compile classpath");
            classpath.addAll(compileClasspath);
            
            List<URL> classPath = new ArrayList<URL>();
            classPath.addAll(urlsOf(classesDirs));
            classPath.addAll(urlsOf(compileClasspath));
            
            log.debug("Classpath for instrumentation is as follows: " + classpath);
            ResourceTransformer dirTransformer = RewritingUtils.createTransformer(
                classPath.toArray(new URL[] {})
            );
            try {
                for (File inputDir : classesDirs) {
                    if (!inputDir.isDirectory()) {
                        continue;
                    }                
                    transformFiles(inputDir, dirTransformer);
                }
            } finally {
                dirTransformer.release();
            }
        } catch (IOException ioe) {
            throw new IllegalStateException("Failed to instrument", ioe);
        }
    }
    
    private List<URL> urlsOf(Set<File> files) {
        if (null == files) {
            return Collections.emptyList();
        }
        List<URL> result = new ArrayList<URL>();
        for (File file : files) {
            URL url = resolveUrl(file);
            if (null == url) {
                continue;
            }                
            result.add(url);
        }
        return result.isEmpty() ? Collections.<URL>emptyList() : Collections.unmodifiableList(result);
    }
    
    private void transformFiles(File inputDirectory, ResourceTransformer dirTransformer) throws IOException {
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
    }

    private URL resolveUrl(File resource) {
        if (!resource.exists()) {
            return null;
        }
        try {
            return resource.toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}