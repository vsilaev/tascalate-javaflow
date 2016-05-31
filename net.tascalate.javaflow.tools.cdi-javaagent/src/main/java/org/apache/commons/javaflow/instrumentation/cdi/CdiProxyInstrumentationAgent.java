package org.apache.commons.javaflow.instrumentation.cdi;

import java.lang.instrument.Instrumentation;

public class CdiProxyInstrumentationAgent {

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
    }

    private static void setupInstrumentation(Instrumentation instrumentation) {
        instrumentation.addTransformer(new CdiProxyClassTransformer(), true);
    }

}
