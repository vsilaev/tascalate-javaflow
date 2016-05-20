package org.apache.commons.javaflow.examples.interceptor;

import org.apache.commons.javaflow.api.ccs;
import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.core.StackRecorder;

public class Execution implements Runnable {
	
	@continuable 
	public void run() {
		TargetInterface target = new TargetClass();
		// Need either @ccs on var or @continuable type
		@ccs InterceptorInterface interceptor = new InterceptorGuard(
			new TransactionalInterceptor(new SecurityInterceptor(target)) 
		);
		String[] array = new String[]{"A", "B", "C"};
		for (int i = 0; i < array.length; i++) {
			System.out.println("Execution " + i);
			interceptor.decorateCall(array[i]);
		}
	}
	
	// Guard is required to balance stack variables
	static class InterceptorGuard implements InterceptorInterface {
		final InterceptorInterface next;
		
		public InterceptorGuard(final InterceptorInterface next) {
			this.next = next;
		}

		public void decorateCall(String param) {
	        StackRecorder stackRecorder = StackRecorder.get();

        	// When restoring we should remove element from the stack
        	// to balance the effect of non-continuable interceptors call
	        // The element removed is a target behind interceptors
	        if (stackRecorder != null && stackRecorder.isRestoring) {
	        	stackRecorder.popReference();
	        }

	        // If there were no intereptors then we will have the following
	        // call here:
	        // TargetClass.execute(String) - this is what we pop-out above
	        
	        
        	next.decorateCall(param);

        	// When capturing we should place self-reference on the stack
        	// to balance the effect of non-continuable interceptors call
	        if (stackRecorder != null && stackRecorder.isCapturing) {
	        	stackRecorder.pushReference(this);
	        }
	        

		}
	}
}
