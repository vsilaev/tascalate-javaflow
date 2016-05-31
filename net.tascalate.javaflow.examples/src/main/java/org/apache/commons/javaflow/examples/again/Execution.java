package org.apache.commons.javaflow.examples.again;

import java.security.SecureRandom;
import java.util.Random;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

public class Execution implements Runnable {
	
	@Override
	public @continuable void run() {
		final Random rnd = new SecureRandom();
		try {
			 Continuation.suspend();
			 // LOOP_START
			 System.out.println("resumed");

			 int r = rnd.nextInt(5);
			 if ( r != 0 ) {
			   System.out.println("do it again, r=" + r);
			   Continuation.again(); // like "GOTO LOOP_START", first statement after closest suspend()
			 }

			 System.out.println("done");
		} finally {
			// This will be called only once
			System.out.println("Finally is called");
		}
	}
}
