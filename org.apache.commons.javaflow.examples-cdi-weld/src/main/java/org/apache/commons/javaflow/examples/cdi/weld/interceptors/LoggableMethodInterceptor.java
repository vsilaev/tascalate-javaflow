package org.apache.commons.javaflow.examples.cdi.weld.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.commons.javaflow.examples.cdi.weld.annotations.LoggableMethod;

@LoggableMethod @Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 3)
public class LoggableMethodInterceptor  {

    @AroundInvoke
    public Object manageSecurityContext(InvocationContext ctx) throws Exception {
        System.out.println("Entering " + ctx.getMethod());
        try {
            return ctx.proceed();
        } finally {
            System.out.println("Exiting " + ctx.getMethod());
        }
    }
}
