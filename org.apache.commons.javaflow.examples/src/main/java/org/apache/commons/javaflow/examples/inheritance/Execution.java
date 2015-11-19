package org.apache.commons.javaflow.examples.inheritance;


import org.apache.commons.javaflow.api.ccs;
import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.api.Continuation;

@continuable 
public class Execution implements Runnable {
	
	@continuable 
	public void run() {
		// Single abstract method of anonymous class inside @Continuable method
		// You may omit TYPE_USE Continuable annotation on anonymous runnable class
		
		// Direct invocation over concrete type
		new Runnable() {
			@Override
			@continuable // meanwhile method annotation is mandatory
			public void run() {
				System.out.println("Anonymous class -- before");
				Continuation.suspend("SAM Interface");
				System.out.println("Anonymous class -- after");
			}
		}.run(); 

		// Invocation over non-continuable supertype/interface
		// Notice that you MUST annotate variable
		@ccs Runnable x = new Runnable() {
			@Override
			@continuable
			public void run() {
				System.out.println("Ref Anonymous class -- before");
				Continuation.suspend("Ref SAM Interface");
				System.out.println("Ref Anonymous class -- after");
			}
		};
		x.run();
		
		@ccs IDemo d1 = createDemo();
		d1.call(11);
		
		DemoConcrete d2 = (DemoConcrete)d1;
		d2.call(77);
		
		try { 
			Integer.valueOf("XYZ");
		} catch (NumberFormatException ex) {
			String message1 = "In ";
			String message2 = "exception ";
			String message3 = "handler";
			System.out.println(message1 + message2 + message3);
			x.run();
		}
		

	}
	
	@continuable 
	void suspend(final int i) {
		System.out.println("Exe before suspend");
		Continuation.suspend(i);
		System.out.println("Exe after suspend");
	}

	private IDemo createDemo() {
		DemoConcrete result = new DemoConcrete();
		result.outer = this;
		return result;
	}

}
