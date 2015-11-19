package org.apache.commons.javaflow.examples.simple;

import org.apache.commons.javaflow.api.Continuation;

public class SimpleExample {


	public static void main(final String[] argv) throws Exception {
	
		for (Continuation cc = Continuation.startWith(new Execution()); null != cc; cc = Continuation.continueWith(cc)) {
			System.out.println("Interrupted " + cc.value());
		}
		
		System.out.println("===");
		
	}


}
