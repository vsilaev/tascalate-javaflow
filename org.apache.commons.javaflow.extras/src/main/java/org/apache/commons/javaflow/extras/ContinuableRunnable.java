package org.apache.commons.javaflow.extras;

import org.apache.commons.javaflow.api.continuable;

public interface ContinuableRunnable extends Runnable {
	//Re-declare to mark as continuable
	@continuable void run();
}
