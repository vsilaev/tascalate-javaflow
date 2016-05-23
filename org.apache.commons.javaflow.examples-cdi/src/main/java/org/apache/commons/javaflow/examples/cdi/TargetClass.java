package org.apache.commons.javaflow.examples.cdi;

import org.apache.commons.javaflow.examples.cdi.annotations.SecureBean;
import org.apache.commons.javaflow.examples.cdi.annotations.TransactionalMethod;
import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;
import org.jboss.weld.environment.se.contexts.ThreadScoped;

@SecureBean
@ThreadScoped
public class TargetClass implements TargetInterface {

    @Override
    @TransactionalMethod
    @continuable 
    public void execute(final String prefix) {
        System.out.println("In target BEFORE suspend");
        final Object value = Continuation.suspend(this + " @ " + prefix);
        System.out.println("In target AFTER suspend: " + value);
    }


}
