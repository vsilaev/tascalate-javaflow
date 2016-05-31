package org.apache.commons.javaflow.examples.cdi.weld.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.commons.javaflow.examples.cdi.weld.annotations.TransactionalMethod;

@TransactionalMethod @Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 1)
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
