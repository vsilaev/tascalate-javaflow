package org.apache.commons.javaflow.core;

/**
 * This exception is used to signal
 * a control flow change that needs
 * the cooperation inside {@link StackRecorder}.
 *
 * <p>This class is only for javaflow internal code.</p>
 *
 * @author Kohsuke Kawaguchi
 */
final class ContinuationDeath extends Error {
    
	private static final long serialVersionUID = 1L;

    public ContinuationDeath() {

    }
    
    public Throwable fillInStackTrace() {
    	return this;
    }

    final static ContinuationDeath INSTANCE = new ContinuationDeath();
}
