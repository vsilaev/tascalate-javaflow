package org.apache.commons.javaflow.examples.cdi.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.commons.javaflow.api.InterceptorSupport;
import org.apache.commons.javaflow.examples.cdi.annotations.ContinuableMethod;

// This is mandatory interceptor to allow continuations
// to work correctly for scoped + intercepted + decorated beans
// You may use it as is in your CDI projects
// Please make sure that yoyr beans are using @ContinuableMethod 
// annotation rather than @continuable because later has no
// @InterceptorBinding for obvious reasons

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
