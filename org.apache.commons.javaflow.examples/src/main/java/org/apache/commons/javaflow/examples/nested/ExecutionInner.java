package org.apache.commons.javaflow.examples.nested;

import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.api.Continuation;

@continuable
public class ExecutionInner implements Runnable {
	final private int i;
	
	public ExecutionInner(int i) {
		this.i = i;
	}
	
	@Override
	@continuable
	public void run() {
		for (char c = 'A'; c < 'E'; c++) {
			StringBuilder v = new StringBuilder();
			v.append(c).append(i);
			System.out.println("\tInner " + v);
			Continuation.suspend(v);
		}
	}
}
