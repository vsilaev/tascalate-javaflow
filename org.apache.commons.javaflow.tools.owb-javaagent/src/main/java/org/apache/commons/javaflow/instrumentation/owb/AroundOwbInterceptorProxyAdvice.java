package org.apache.commons.javaflow.instrumentation.owb;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

class AroundOwbInterceptorProxyAdvice extends AroundOwbProxyInvokeAdvice {
    
    final private Type proxiedInstanceType;

    AroundOwbInterceptorProxyAdvice(int api, MethodVisitor mv, int acc, String className, String methodName, String desc, Type proxiedInstanceType) {
        super(api, mv, acc, className, methodName, desc);
        this.proxiedInstanceType = proxiedInstanceType;
    }

    @Override
    protected void loadProxiedInstance() {
        loadThis();
        getField(Type.getType(className), FIELD_PROXIED_INSTANCE, proxiedInstanceType);
    }
    
    static final String FIELD_PROXIED_INSTANCE = "owbIntDecProxiedInstance"; //InterceptorDecoratorProxyFactory.FIELD_PROXIED_INSTANCE;

}
