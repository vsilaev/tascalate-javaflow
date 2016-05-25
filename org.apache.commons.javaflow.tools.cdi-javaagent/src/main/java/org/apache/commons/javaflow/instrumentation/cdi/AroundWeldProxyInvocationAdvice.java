package org.apache.commons.javaflow.instrumentation.cdi;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

class AroundWeldProxyInvocationAdvice extends AroundCdiProxyInvocationAdvice {
    protected AroundWeldProxyInvocationAdvice(int api, MethodVisitor mv, int acc, String className, String methodName, String desc) {
        super(api, mv, acc, className, methodName, desc);
    }

    @Override
    protected void loadProxiedInstance() {
        loadThis();
        invokeVirtual(Type.getType(className), Method.getMethod("Object getTargetInstance()"));
    }

}
