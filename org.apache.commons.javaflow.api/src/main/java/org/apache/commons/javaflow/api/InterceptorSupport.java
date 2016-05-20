package org.apache.commons.javaflow.api;

import org.apache.commons.javaflow.core.StackRecorder;

final public class InterceptorSupport {
    private InterceptorSupport() {}
    
    public static Object beforeExecution() {
        StackRecorder stackRecorder = StackRecorder.get();

        // When restoring we should remove element from the stack
        // to balance the effect of non-continuable interceptors call
        // The element removed is a target behind interceptors
        if (stackRecorder != null && stackRecorder.isRestoring) {
            return stackRecorder.popReference();
        } else {
            return null;
        }
    }
    
    public static void afterExecution(final Object interceptor) {
        StackRecorder stackRecorder = StackRecorder.get();
        
        // When capturing we should place self-reference on the stack
        // to balance the effect of non-continuable interceptors call
        if (stackRecorder != null && stackRecorder.isCapturing) {
            stackRecorder.pushReference(interceptor);
        }
    }
}
