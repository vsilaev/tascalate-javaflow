package org.apache.commons.javaflow.spi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StopException extends RuntimeException {

    final private static long serialVersionUID = 1L;

    private StopException() { }
    
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    public static final StopException INSTANCE = new StopException();
    
    public static boolean __dirtyCheckSkipContinuationsOnClass(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (null != interfaces) {
            for (final String intf : interfaces){
                if (PROXY_MARKER_INTERFACES.contains(intf)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static final Set<String> PROXY_MARKER_INTERFACES = new HashSet<String>(Arrays.asList(
            "org/apache/webbeans/proxy/OwbInterceptorProxy", 
            "org/apache/webbeans/proxy/OwbNormalScopeProxy",
            "org/jboss/weld/bean/proxy/ProxyObject"
    )); 
}
