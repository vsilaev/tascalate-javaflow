package org.apache.commons.javaflow.examples.cdi.owb.interceptors;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.commons.javaflow.examples.cdi.owb.annotations.SecureBean;

@SecureBean @Interceptor
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
