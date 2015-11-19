package org.apache.commons.javaflow.examples.simple;

import java.util.Arrays;

import org.apache.commons.javaflow.api.Continuation;

@MyContinuable 
public class Execution implements Runnable {
	
	@Override
	@MyContinuable 
	public void run() {
		final Object[] array = new String[]{"A", "C", "B"};
		for (int i = 1; i <= 5; i++) {
			System.out.println("Execution " + i);
			Arrays.sort(array);
			suspend(i);
		}
	}
	
	@MyContinuable 
	public void suspend(final int i) {
		System.out.println("Exe before suspend");
		Continuation.suspend(i);
		System.out.println("Exe after suspend");
	}
}
