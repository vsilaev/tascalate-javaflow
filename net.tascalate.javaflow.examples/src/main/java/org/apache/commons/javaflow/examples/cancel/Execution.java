package org.apache.commons.javaflow.examples.cancel;

import java.util.Arrays;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

public class Execution implements Runnable {
	
	@Override
	public @continuable void run() {
		final Object[] array = new String[]{"A", "C", "B"};
		try {
			int i = 1;
			System.out.println("Before suspend");
			Continuation.suspend("XYZ");
			System.out.println("After suspend #" + (i++) + ", should not mutate!!!");
			Continuation.cancel();
			// The line below will never be called -- 
			// first we are canceling continuation
			// then we are destroying it
			array[1] = "CHANGED";
		} finally {
			// This will be called only after cc.destroy() from outer code
			System.out.println("Finally is called, array value is " + Arrays.asList(array));
		}
	}
}
