package org.apache.commons.javaflow.examples.cancel;

import org.apache.commons.javaflow.api.Continuation;

public class CancelExample {


	public static void main(final String[] argv) throws Exception {
		Continuation cc = Continuation.startSuspendedWith(new Execution());
		cc = cc.resume();
		System.out.println("In main, first stop, let's loop (cc.value = " + cc.value() + ") ");
		for (int i = 1; i <= 3; i++) {
			cc = cc.resume();		
			System.out.println("In main after #" + i + " suspend (cc.value = " + cc.value() + ") ");
		}
		// This will gracefully complete continuation -- finally blocks will be executed
		cc.destroy();
		System.out.println("In main after destroy");
		System.out.println("===");
	}


}
