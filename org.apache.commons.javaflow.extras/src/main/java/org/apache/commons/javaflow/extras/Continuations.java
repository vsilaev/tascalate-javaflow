package org.apache.commons.javaflow.extras;

import org.apache.commons.javaflow.api.Continuation;

final public class Continuations {
	private Continuations() {}
	
	public static Continuation create(ContinuableRunnable o) {
		return Continuation.startSuspendedWith(o);
	}
	
	public static Continuation start(ContinuableRunnable o, Object ctx) {
		return Continuation.startWith(o, ctx);
	}
	
	public static Continuation start(ContinuableRunnable o) {
		return Continuation.startWith(o);
	}
}
