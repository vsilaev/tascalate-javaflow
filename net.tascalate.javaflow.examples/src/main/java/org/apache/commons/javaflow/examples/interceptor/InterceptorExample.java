package org.apache.commons.javaflow.examples.interceptor;

import org.apache.commons.javaflow.api.Continuation;

public class InterceptorExample {


	public static void main(final String[] argv) throws Exception {
	
		int i = 0;
		for (Continuation cc = Continuation.startWith(new Execution()); null != cc; cc = cc.resume(i += 100)) {
			System.out.println("SUSPENDED " + cc.value());
		}
		
		System.out.println("===");
		
	}


}
