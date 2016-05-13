package org.apache.commons.javaflow.examples.nested;

import org.apache.commons.javaflow.api.Continuation;

public class NestedExample {


	public static void main(final String[] argv) throws Exception {
	
		for (Continuation cc = Continuation.startWith(new ExecutionOuter()); null != cc; cc = cc.resume()) {
			System.out.println("Interrupted " + cc.value());
		}
		
		System.out.println("===");
		
	}


}
