package org.apache.commons.javaflow.examples.cdi.owb.interceptors;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.commons.javaflow.examples.cdi.owb.annotations.TransactionalMethod;

@TransactionalMethod @Interceptor
public class TransactionalMethodInterceptor  {

    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Throwable {
        System.out.println("Begin transaction...");
        boolean success = true;
        try {
            return ctx.proceed();
        } catch (final Throwable ex) {
            System.out.println("...Rollback transaction");
            success = false;
            throw ex;
        } finally {
            if (success) {
                System.out.println("...Commit transaction");
            }
        }
    }
}
