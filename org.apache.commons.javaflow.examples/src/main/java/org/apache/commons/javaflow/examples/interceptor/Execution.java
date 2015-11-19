package org.apache.commons.javaflow.examples.interceptor;

import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.core.StackRecorder;

@continuable
public class Execution implements Runnable {
	
	@continuable 
	public void run() {
		TargetInterface target = new TargetClass();
		InterceptorGuard interceptor = new InterceptorGuard(new AroundInvokeInterceptor(target));
		String[] array = new String[]{"A", "B", "C"};
		for (int i = 0; i < array.length; i++) {
			System.out.println("Execution " + i);
			interceptor.decorateCall(array[i]);
		}
	}
	
	
	@continuable
	// Guard is required to balance stack variables
	static class InterceptorGuard implements InterceptorInterface {
		final InterceptorInterface next;
		
		public InterceptorGuard(final InterceptorInterface next) {
			this.next = next;
		}

		@continuable
		public void decorateCall(String param) {
	        StackRecorder stackRecorder = StackRecorder.get();

        	next.decorateCall(param);
        	
	        if (stackRecorder != null && stackRecorder.isCapturing) {
	        	// When capturing we should place self-reference on the stack
	        	// to balance the effect of non-continuable interceptors call
	        	stackRecorder.pushReference(this);
	        }

		}
	}
}
