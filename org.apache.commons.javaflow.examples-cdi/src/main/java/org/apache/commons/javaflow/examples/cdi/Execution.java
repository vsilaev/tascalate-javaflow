package org.apache.commons.javaflow.examples.cdi;

import javax.inject.Inject;

import org.apache.commons.javaflow.examples.cdi.annotations.ContinuableMethod;

public class Execution implements Runnable {

    @Inject
    TargetClass target;

    @ContinuableMethod 
    public void run() {
        // Need either @ccs on var or @continuable type
        // if Injected field is a non-continuable intf.
        // This sucks, will be fixed later
        //@ccs TargetInterface target = this.target;

        String[] array = new String[]{"A", "B", "C"};
        for (int i = 0; i < array.length; i++) {
            System.out.println("Execution " + i);
            target.execute(array[i]);
        }
    }
}
