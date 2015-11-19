package org.apache.commons.javaflow.examples.nested;

import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.api.Continuation;

@continuable 
public class ExecutionOuter implements Runnable {
	
	@Override
	@continuable 
	public void run() {
		for (int i = 1; i <= 5; i++) {
			System.out.println("Execution " + i);
			for (Continuation cc = Continuation.startWith(new ExecutionInner(i)); null != cc; cc = Continuation.continueWith(cc)) {
				
				System.out.println("\tFrom inner " + cc.value());
				Continuation.suspend(i);
			}
		}
	}

}
