package org.apache.commons.javaflow.examples.interceptor;

import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.api.Continuation;

public class TargetClass implements TargetInterface {

	@Override
	public @continuable void execute(final String prefix) {
		System.out.println("In target BEFORE suspend");
		final Object value = Continuation.suspend("Target @ " + prefix);
		System.out.println("In target AFTER suspend: " + value);
	}

	
}
