package org.apache.commons.javaflow.examples.cdi.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.commons.javaflow.api.InterceptorSupport;
import org.apache.commons.javaflow.examples.cdi.annotations.ContinuableMethod;

@ContinuableMethod @Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE - 1)
public class ContinuableMethodInterceptor {
    
    @AroundInvoke
    public Object continuationTrampoline(InvocationContext ctx) throws Throwable {
        if (InterceptorSupport.isInstrumented(ctx.getTarget())) {
            // Stack is valid, no need to corrupt it :)))
            return ctx.proceed();
        } else {
            // Stack has non-continuable interceptors "gap"
            // Fix it
            InterceptorSupport.beforeExecution();
            try {
                return ctx.proceed();
            } finally {
                InterceptorSupport.afterExecution(this);
            }
        }
    }
}
