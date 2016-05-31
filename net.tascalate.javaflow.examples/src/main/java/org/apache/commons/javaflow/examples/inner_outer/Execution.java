package org.apache.commons.javaflow.examples.inner_outer;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

public class Execution implements Runnable {

    @Override
    public @continuable void run() {
        Inner inner = new Inner();
        for (int i = 1; i <= 5; i++) {
            inner.innerMethod(i);
        }
    }
    
    // Private, to show that accessor$### methods are instrumented
    private @continuable void outerMethod(int i) {
        System.out.println("Exe before suspend");
        Object fromCaller = Continuation.suspend(i);
        System.out.println("Exe after suspend: " + fromCaller);   
    }
    
    class Inner {
        // Private, to show that accessor$### methods are instrumented
        private @continuable void innerMethod(int i) {
            outerMethod(i);
        }
    }
}
