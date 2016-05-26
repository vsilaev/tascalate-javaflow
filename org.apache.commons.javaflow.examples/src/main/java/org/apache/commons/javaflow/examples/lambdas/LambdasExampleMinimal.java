package org.apache.commons.javaflow.examples.lambdas;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.extras.Continuations;

// We need to implement mixin to inherit at least one @continuable method
// In this case -- marker method Continuations.__()
// This is necessary to instrument desugared lambda bodies (in this class)
// Plus mixin provides convinient methods to create/start continuation
// out of Runnable lambda
public class LambdasExampleMinimal {

    public static void main(final String[] argv) throws Exception {

        Continuation cc = Continuations.start(() -> {
            for (int i = 1; i <= 5; i++) {
                System.out.println("Exe before suspend");
                Continuation.suspend(i);
                System.out.println("Exe after suspend");	        
            }
        });

        for (; null != cc; cc = cc.resume()) {
            System.out.println("Interrupted " + cc.value());
        }

        System.out.println("===");
    }


}
