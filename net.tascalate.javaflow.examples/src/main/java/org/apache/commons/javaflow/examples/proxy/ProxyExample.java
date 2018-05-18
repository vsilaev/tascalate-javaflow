package org.apache.commons.javaflow.examples.proxy;

import java.lang.reflect.Proxy;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

public class ProxyExample {

    public static void main(String[] args) {
        Execution execution = (Execution) Proxy.newProxyInstance(
            ProxyExample.class.getClassLoader(), 
            new Class<?>[]{Execution.class}, 
            new LoggingInvocationHandler(new TargetClass()));
        
        System.out.println(execution);
        System.out.println(">>Non-continuable outside continuation: " + execution.nonContinuableTest());
        System.out.println("======");
        
        Runnable runnable = new Invoker(execution);
        for (Continuation cc = Continuation.startWith(runnable); null != cc;) {
            final String valueFromContinuation = String.class.cast(cc.value());
            System.out.println(">>Interrupted " + valueFromContinuation);
            // Let's continuation resume
            cc = cc.resume("processed-" + valueFromContinuation);
        }

        System.out.println("ALL DONE");

    }

    static class Invoker implements Runnable {
        final private Execution execution;
        
        Invoker(Execution execution) {
            this.execution = execution;
        }
        
        public @continuable void run() {
            System.out.println(">>Non-continuable inside continuation: " + execution.nonContinuableTest());
            execution.execute();
        }
    }
}
