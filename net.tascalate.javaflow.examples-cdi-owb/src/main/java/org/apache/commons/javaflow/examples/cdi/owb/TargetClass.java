package org.apache.commons.javaflow.examples.cdi.owb;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.examples.cdi.owb.annotations.LoggableMethod;
import org.apache.commons.javaflow.examples.cdi.owb.annotations.SecureBean;
import org.apache.commons.javaflow.examples.cdi.owb.annotations.TransactionalMethod;

@SecureBean
@ApplicationScoped
public class TargetClass implements TargetInterface {

    @Override
    @TransactionalMethod
    public @continuable void execute(final String prefix) {
        executeNested(prefix);
    }

    @LoggableMethod
    protected @continuable void executeNested(final String prefix) {
        System.out.println("In target BEFORE suspend");
        final Object value = Continuation.suspend(this + " @ " + prefix);
        System.out.println("In target AFTER suspend: " + value);
    }
    
}
