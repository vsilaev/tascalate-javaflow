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
package org.apache.commons.javaflow.agent.proxy;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.javaflow.agent.common.ConfigurableClassFileTransformer;
import org.apache.commons.javaflow.spi.InstrumentationUtils;

import net.tascalate.instrument.agent.AbstractInstrumentationAgent;

public class ContinuableProxiesInstrumentationAgent extends AbstractInstrumentationAgent {
    
    protected ContinuableProxiesInstrumentationAgent(String arguments, Instrumentation instrumentation) {
        super(arguments, instrumentation);
    }

    /**
     * JVM hook to statically load the javaagent at startup.
     * 
     * After the Java Virtual Machine (JVM) has initialized, the premain method
     * will be called. Then the real application main method will be called.
     * 
     * @param args
     * @param instrumentation
     * @throws Exception
     */
    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        ContinuableProxiesInstrumentationAgent agent = new ContinuableProxiesInstrumentationAgent(args, instrumentation);
        agent.install();
    }

    /**
     * JVM hook to dynamically load javaagent at runtime.
     * 
     * The agent class may have an agentmain method for use when the agent is
     * started after VM startup.
     * 
     * @param args
     * @param instrumentation
     * @throws Exception
     */
    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        ContinuableProxiesInstrumentationAgent agent = new ContinuableProxiesInstrumentationAgent(args, instrumentation);
        Set<String> nonRetransformPackages = new HashSet<String>(BASE_OWN_PACKAGES);
        nonRetransformPackages.addAll(
            InstrumentationUtils.packagePrefixesOf(
                InstrumentationUtils.class, 
                ConfigurableClassFileTransformer.class
            )
        );
        agent.attach(nonRetransformPackages);
    }
    
    @Override
    protected Collection<ClassFileTransformer> createTransformers(boolean canRetransform) {
        if (canRetransform) {
            ClassFileTransformer transformer = new ContinuableProxyBytecodeTransformer();
            return Collections.singleton(transformer);
        } else {
            return Collections.emptySet();
        }
    }

}