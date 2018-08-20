/**
 * ﻿Copyright 2013-2017 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.instrumentation;

import java.lang.instrument.Instrumentation;

public class JavaFlowInstrumentationAgent {

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
        setupInstrumentation(instrumentation);
        System.setProperty(JavaFlowInstrumentationAgent.class.getName(), "true");
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
        setupInstrumentation(instrumentation);
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (instrumentation.isModifiableClass(clazz) && 
                !JavaFlowClassTransformer.skipClassByName(clazz.getName())) {
                try {
                    instrumentation.retransformClasses(clazz);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        System.setProperty(JavaFlowInstrumentationAgent.class.getName(), "true");        
    }

    private static void setupInstrumentation(Instrumentation instrumentation) {
        instrumentation.addTransformer(new JavaFlowClassTransformer(), true);
    }

}
