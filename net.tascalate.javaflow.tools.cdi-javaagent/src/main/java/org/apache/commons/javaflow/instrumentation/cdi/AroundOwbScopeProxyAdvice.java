package org.apache.commons.javaflow.instrumentation.cdi;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

class AroundOwbScopeProxyAdvice extends AroundCdiProxyInvocationAdvice {

    final private Type proxiedInstanceProviderType; 

    public AroundOwbScopeProxyAdvice(int api, MethodVisitor mv, int acc, String className, String methodName, String desc, Type proxiedInstanceProviderType) {
        super(api, mv, acc, className, methodName, desc);
        this.proxiedInstanceProviderType = proxiedInstanceProviderType;
    }

    @Override
    protected void loadProxiedInstance() {
        loadThis();
        getField(Type.getType(className), FIELD_INSTANCE_PROVIDER, proxiedInstanceProviderType);
        invokeInterface(PROVIDER, PROVIDER_GET);
    }

    private static final Type PROVIDER = Type.getType("javax/inject/Provider");
    private static final Method PROVIDER_GET = Method.getMethod("java.lang.Object get()");

    static final String FIELD_INSTANCE_PROVIDER = "owbContextualInstanceProvider";//NormalScopeProxyFactory.FIELD_INSTANCE_PROVIDER;
}
