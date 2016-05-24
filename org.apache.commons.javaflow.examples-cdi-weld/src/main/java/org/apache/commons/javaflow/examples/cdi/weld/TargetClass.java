package org.apache.commons.javaflow.examples.cdi.weld;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.examples.cdi.weld.annotations.LoggableMethod;
import org.apache.commons.javaflow.examples.cdi.weld.annotations.SecureBean;
import org.apache.commons.javaflow.examples.cdi.weld.annotations.TransactionalMethod;
import org.jboss.weld.environment.se.contexts.ThreadScoped;

@SecureBean
@ThreadScoped
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
