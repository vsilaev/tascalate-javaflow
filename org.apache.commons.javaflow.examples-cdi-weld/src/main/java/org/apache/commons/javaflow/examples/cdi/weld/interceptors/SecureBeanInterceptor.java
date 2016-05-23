package org.apache.commons.javaflow.examples.cdi.weld.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.commons.javaflow.examples.cdi.weld.annotations.SecureBean;

@SecureBean @Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 2)
public class SecureBeanInterceptor  {

    @AroundInvoke
    public Object manageSecurityContext(InvocationContext ctx) throws Exception {
        System.out.println("Security Interceptor before call");
        try {
            return ctx.proceed();
        } finally {
            System.out.println("Security Interceptor after call");
        }
    }
}
