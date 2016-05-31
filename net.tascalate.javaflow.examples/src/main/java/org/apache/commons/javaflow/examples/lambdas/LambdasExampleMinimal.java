package org.apache.commons.javaflow.examples.lambdas;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.extras.Continuations;

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
