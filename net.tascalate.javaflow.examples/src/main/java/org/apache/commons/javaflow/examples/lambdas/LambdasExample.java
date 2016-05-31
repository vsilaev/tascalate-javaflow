package org.apache.commons.javaflow.examples.lambdas;

import static org.apache.commons.javaflow.examples.lambdas.ContinuableAdapters.accept;
import static org.apache.commons.javaflow.examples.lambdas.ContinuableAdapters.exec;
import static org.apache.commons.javaflow.examples.lambdas.ContinuableAdapters.forEach;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.javaflow.api.ccs;
import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.extras.ContinuableRunnable;
import org.apache.commons.javaflow.extras.Continuations;

public class LambdasExample {
    public static void main(final String[] argv) throws Exception {
        LambdasExample example = new LambdasExample();

        for (Continuation cc = Continuations.start(example::executeAll); null != cc; cc = cc.resume()) {
            System.out.println("Interrupted " + cc.value());
        }

        System.out.println("===");

    }

    String ref = " ** ";

    private @continuable void executeAll() {

        //  @Continuable method references and @Continuable SAM interfaces are supported
        ContinuableRunnable r1 = this::lambdaDemo;
        r1.run();

        ContinuableRunnable r2 = () -> {
            System.out.println("ContinuableRunnable by arrow function -- before");
            Continuation.suspend(" ** ContinuableRunnable by arrow function");
            System.out.println("ContinuableRunnable by arrow function -- after");
        };
        r2.run();

        // Lambda reference MUST have annotated CallSite if SAM interface is not @continuable
        // Notice that we still MUST create right interface (ContinuableRunnable in this case)
        @ccs Runnable closure = exec( () -> {
            System.out.println("Plain Runnable Lambda by arrow function -- before");
            Continuation.suspend(" ** Plain Runnable Lambda by arrow function" + ref);
            System.out.println("Plain Runnable Lambda by arrow function -- after");
        } );
        closure.run();

        // We can't use stream.forEach as is, but we can provide good enough helpers
        // See org.apache.commons.javaflow.examples.lambdas.ContinuableAdapters for
        // definition of "accept" & "forEach"
        List<String> listOfStrings = Arrays.asList("A", null, "B", null, "C"); 
        forEach( 
                listOfStrings.stream().filter(s -> s != null).map(s -> s + s), 
                accept(this::yieldString1).andThen(this::yieldString2) 
        );

    }

    // Must be annotated to be used as lambda by method-ref
    @continuable void lambdaDemo() {
        System.out.println("Lambda by method reference -- before");
        Continuation.suspend(" ** Lambda by method reference** ");
        System.out.println("Lambda by method reference -- after");
    }

    @continuable void yieldString1(String s) {
        System.out.println("Before yield I " + s);
        Continuation.suspend("yield I " + s);
        System.out.println("After yield I " + s);
    }

    @continuable void yieldString2(String s) {
        System.out.println("Before yield II");
        Continuation.suspend("yield II " + s);
        System.out.println("After yield II");
    }


}
