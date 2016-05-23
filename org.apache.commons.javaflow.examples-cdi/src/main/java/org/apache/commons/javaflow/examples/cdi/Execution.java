package org.apache.commons.javaflow.examples.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.javaflow.api.continuable;
import org.jboss.weld.bean.proxy.ProxyObject;

@ApplicationScoped
public class Execution implements Runnable {

    @Inject
    TargetInterface target;

    @continuable 
    public void run() {
    	System.out.println("Target is proxy? " + (target instanceof ProxyObject));
        String[] array = new String[]{"A", "B", "C"};
        for (int i = 0; i < array.length; i++) {
            System.out.println("Execution " + i);
            target.execute(array[i]);
        }
    }
}
