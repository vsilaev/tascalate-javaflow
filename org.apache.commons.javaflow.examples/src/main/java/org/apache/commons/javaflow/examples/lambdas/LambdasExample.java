package org.apache.commons.javaflow.examples.lambdas;

import static org.apache.commons.javaflow.examples.lambdas.ContinuableAdapters.apply;
import static org.apache.commons.javaflow.examples.lambdas.ContinuableAdapters.from;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.javaflow.api.ccs;
import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.api.Continuation;

public class LambdasExample {
	public static void main(final String[] argv) throws Exception {
		LambdasExample example = new LambdasExample();
		
		for (Continuation cc = Continuation.startWith(example::run); null != cc; cc = Continuation.continueWith(cc)) {
			System.out.println("Interrupted " + cc.value());
		}
		
		System.out.println("===");

	}
	
	String ref = " ** ";
	
	private @continuable void run() {
		
		//  @Continuable method references and @Continuable SAM interfaces are supported
		ContinuableRunnable r1 = this::lambdaDemo;
		r1.run();
		
		ContinuableRunnable r2 = () -> {
			System.out.println("ContinuableRunnable by arrow function -- before");
			Continuation.suspend(" ** ContinuableRunnable by arrow function");
			System.out.println("ContinuableRunnable by arrow function -- after");
		};
		r2.run();

		// Lambda reference MUST have annotated CallSite if SAM interface is not @continuable
		@ccs Runnable closure = () -> {
			System.out.println("Lambda by arrow function -- before");
			Continuation.suspend(" ** Lambda by arrow function" + ref);
			System.out.println("Lambda by arrow function -- after");
		};
		closure.run();
		
		// Lambda reference MUST have annotated CallSite if SAM interface is not @continuable
		// Arrays are supported as well
		@ccs Runnable[] exe = {this::lambdaDemo};
		exe[0].run();


		// Unfortunately, any default methods should be re-wrapped like below
		// See org.apache.commons.javaflow.examples.lambdas.ContinuableAdapters
		List<String> listOfStrings = Arrays.asList("A", "B", "C"); 
		
		from(listOfStrings).forEach( 
			apply(this::yieldString1).andThen(apply(this::yieldString2)) 
		);
		
	}
	
	// Must be annotated to be used as lambda by method-ref
	@continuable void lambdaDemo() {
		System.out.println("Lambda by method reference -- before");
		Continuation.suspend(" ** Lambda by method reference** ");
		System.out.println("Lambda by method reference -- after");
	}
	
	@continuable void yieldString1(String s) {
		System.out.println("Before yield I " + s);
		Continuation.suspend("yield I " + s);
		System.out.println("After yield I " + s);
	}
	
	@continuable void yieldString2(String s) {
		System.out.println("Before yield II");
		Continuation.suspend("yield II " + s);
		System.out.println("After yield II");
	}

	
}
