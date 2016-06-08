package org.apache.commons.javaflow.extras;

import org.apache.commons.javaflow.api.continuable;

/**
 * Continuable version of Runnable 
 */
@FunctionalInterface
public interface ContinuableRunnable extends Runnable {
	/**
	 * Run method re-declared as continuable
	 */
	@continuable void run();
}
