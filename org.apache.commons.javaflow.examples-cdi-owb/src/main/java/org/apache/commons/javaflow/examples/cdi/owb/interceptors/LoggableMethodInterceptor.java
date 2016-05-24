package org.apache.commons.javaflow.examples.cdi.owb.interceptors;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.commons.javaflow.examples.cdi.owb.annotations.LoggableMethod;

@LoggableMethod @Interceptor
public class LoggableMethodInterceptor  {

    @AroundInvoke
    public Object manageSecurityContext(InvocationContext ctx) throws Exception {
        System.out.println("Enetering " + ctx.getMethod());
        try {
            return ctx.proceed();
        } finally {
            System.out.println("Exiting " + ctx.getMethod());
        }
    }
}
