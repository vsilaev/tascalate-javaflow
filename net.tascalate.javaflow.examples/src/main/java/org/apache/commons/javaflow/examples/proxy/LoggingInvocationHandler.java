package org.apache.commons.javaflow.examples.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

import org.apache.commons.javaflow.api.InterceptorSupport;

public class LoggingInvocationHandler implements InvocationHandler {
    
    final private Object delegate;
    
    public LoggingInvocationHandler(Object delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            String methodName = method.getName();
            if ("equals".equals(methodName) && method.getReturnType() == boolean.class && method.getParameterTypes().length == 1) {
                if (null != args[0] && Proxy.isProxyClass(args[0].getClass())) {
                    InvocationHandler otherHandler = Proxy.getInvocationHandler(args[0]);
                    if (otherHandler instanceof LoggingInvocationHandler) {
                        return Objects.equals(delegate, ((LoggingInvocationHandler)otherHandler).delegate);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else if ("hashCode".equals(methodName) && method.getReturnType() == int.class && method.getParameterTypes().length == 0) {
                return Objects.hash(delegate) * 37 + System.identityHashCode(this);
            } else if ("toString".equals(methodName) && method.getReturnType() == String.class && method.getParameterTypes().length == 0) {
                return "<proxy[" + this.toString() + "]>{" + delegate + "}";
            } else {
                throw new NoSuchMethodError(method.toString());
            }
        } else {
            System.out.println("::Entering method " + method);
            // Need to balance reference stack due to intermediate
            // non-continuable calls
            InterceptorSupport.beforeExecution(delegate);
            try {
                return method.invoke(delegate, args);
            } finally {
                // Need to balance reference stack due to intermediate
                // non-continuable calls, pass proxied target
                InterceptorSupport.afterExecution(proxy);
                System.out.println("::Exiting method " + method);
            }
        }
    }

}
