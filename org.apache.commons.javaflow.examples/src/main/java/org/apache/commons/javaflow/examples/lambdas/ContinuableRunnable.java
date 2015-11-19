package org.apache.commons.javaflow.examples.lambdas;

import org.apache.commons.javaflow.api.continuable;

@continuable
interface ContinuableRunnable extends Runnable {
	//Re-declare to mark as continuable
	@continuable void run();
}
