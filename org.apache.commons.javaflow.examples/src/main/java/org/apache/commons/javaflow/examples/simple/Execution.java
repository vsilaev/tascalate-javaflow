package org.apache.commons.javaflow.examples.simple;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

public class Execution implements Runnable {

    @Override
    public @continuable void run() {
        for (int i = 1; i <= 5; i++) {
            System.out.println("Exe before suspend");
            Object fromCaller = Continuation.suspend(i);
            System.out.println("Exe after suspend: " + fromCaller);	        
        }
    }
}
