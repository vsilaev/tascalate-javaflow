package org.apache.commons.javaflow.examples.proxy;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

public class TargetClass implements Execution {

    public @continuable void execute() {
        for (String s : new String[] {"A", "B", "C"}) {
            System.out.println("Before suspend: " + s);
            Object ret = Continuation.suspend(s);
            System.out.println("After suspend: " + s + ", reply is: " + ret);
        }
    }
    
    public String nonContinuableTest() {
        return "This is " + this;
    }
}
