package org.apache.commons.javaflow.instrumentation.cdi;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.javaflow.spi.ContinuableClassInfo;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.apache.commons.javaflow.spi.StopException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;

class CdiProxyClassAdapter extends ClassVisitor {

    private String className;
    private Type owbProxiedInstanceType;
    private Type owbProxiedInstanceProviderType;
    private boolean isWeldProxy;
    private ContinuableClassInfo classInfo;

    private final ContinuableClassInfoResolver cciResolver;

    CdiProxyClassAdapter(ClassVisitor delegate, ContinuableClassInfoResolver cciResolver) {
        super(Opcodes.ASM5, delegate);
        this.cciResolver = cciResolver;
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        boolean hasMarker = false;
        for (final String interfaze : interfaces) {
            if (MARKER_INTERFACES.contains(interfaze)) {
                if (WELD_PROXY_OBJECT.equals(interfaze)) {
                    if (!className.endsWith("$$_WeldSubclass")) {
                        // *$$_WeldSubclass is not a scope/interceptor proxy
                        // Otherwise it's indeed a proxy
                        isWeldProxy = true;
                        hasMarker = true;
                    }
                } else {
                    hasMarker = true;
                }
                // Exclusive, may exit early
                break;
            }
        }

        if (!hasMarker) {
            throw StopException.INSTANCE;
        }

        try {
            classInfo = cciResolver.resolve(superName);
            if (null == classInfo) {
                throw StopException.INSTANCE;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (AroundOwbInterceptorProxyAdvice.FIELD_PROXIED_INSTANCE.equals(name)) {
            owbProxiedInstanceType = Type.getType(desc);
        }
        if (AroundOwbScopeProxyAdvice.FIELD_INSTANCE_PROVIDER.equals(name)) {
            owbProxiedInstanceProviderType = Type.getType(desc);
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int acc, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(acc, name, desc, signature, exceptions);
        if (isContinuableMethodProxy(acc, name, desc, signature, exceptions)) {
            if (isWeldProxy) {
                mv = new AroundWeldProxyInvocationAdvice(api, mv, acc, className, name, desc);
            } else if (null != owbProxiedInstanceType) {
                mv = new AroundOwbInterceptorProxyAdvice(api, mv, acc, className, name, desc, owbProxiedInstanceType); 
            } else if (null != owbProxiedInstanceProviderType) {
                mv = new AroundOwbScopeProxyAdvice(api, mv, acc, className, name, desc, owbProxiedInstanceProviderType); 
            }
        }
        return mv;
    }

    protected boolean isContinuableMethodProxy(int acc, String name, String desc, String signature, String[] exceptions) {
        int idx = name.lastIndexOf("$$super");
        if (idx > 0) {
            name = name.substring(0, idx);
        }
        return ! "<init>".equals(name) && classInfo.isContinuableMethod(acc, name, desc, signature);
    }

    private static final String OWB_INTERCEPTOR_PROXY  = "org/apache/webbeans/proxy/OwbInterceptorProxy";
    private static final String OWB_NORMAL_SCOPE_PROXY = "org/apache/webbeans/proxy/OwbNormalScopeProxy";
    private static final String WELD_PROXY_OBJECT      = "org/jboss/weld/bean/proxy/ProxyObject";

    private static final Set<String> MARKER_INTERFACES = new HashSet<String>(Arrays.asList(
            OWB_INTERCEPTOR_PROXY, OWB_NORMAL_SCOPE_PROXY, WELD_PROXY_OBJECT
            )); 
}