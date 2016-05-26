package org.apache.commons.javaflow.examples.inner_outer;

import org.apache.commons.javaflow.api.Continuation;

public class InnerOuterExample {


    public static void main(final String[] argv) throws Exception {

        final String[] strings = {"A", "B", "C"};
        for (Continuation cc = Continuation.startWith(new Execution()); null != cc; ) {
            final int valueFromContinuation = (Integer)cc.value();
            System.out.println("Interrupted " + valueFromContinuation);
            // Let's continuation resume
            cc = cc.resume( strings[valueFromContinuation % strings.length] );
        }

        System.out.println("ALL DONE");

    }


}
