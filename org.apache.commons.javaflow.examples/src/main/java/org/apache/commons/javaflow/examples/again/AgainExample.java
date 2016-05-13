package org.apache.commons.javaflow.examples.again;

import org.apache.commons.javaflow.api.Continuation;

public class AgainExample {


	public static void main(final String[] argv) throws Exception {
		Continuation cc = Continuation.startWith(new Execution());
		System.out.println("In main loop after prologue");
		cc = cc.resume(); // will loop in Execution due to call to again()
		System.out.println("In main done");
		System.out.println("===");
	}


}
