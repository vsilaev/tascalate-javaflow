package org.apache.commons.javaflow.examples.interceptor;

import org.apache.commons.javaflow.api.InterceptorSupport;
import org.apache.commons.javaflow.api.ccs;
import org.apache.commons.javaflow.api.continuable;

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
		    InterceptorSupport.beforeExecution();
		    try {
    	        // If there were no interceptors then we will have the following
    	        // call here:
    	        // TargetClass.execute(String) - this is what we pop-out above
    		    // with InterceptorSupport.beforeExecution();
            	next.decorateCall(param);
		    } finally {
              InterceptorSupport.afterExecution(this);  
            }
		}
	}
}
