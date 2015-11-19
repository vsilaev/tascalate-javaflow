package org.apache.commons.javaflow.core;

/**
 * This exception is used to signal
 * a control flow change that needs
 * the cooperation inside {@link StackRecorder}.
 *
 * <p>
 * This class is only for javaflow internal code.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ContinuationDeath extends Error {
    
	private static final long serialVersionUID = 1L;

	final public String mode;

    public ContinuationDeath(String mode) {
        this.mode = mode;
    }

    /**
     * Signals that the continuation wants to exit the execution.
     */
    public static final String MODE_EXIT = "exit";
    /**
     * Signals that the execution should restart immediately
     * from where it resumed.
     */
    public static final String MODE_AGAIN = "again";
    /**
     * Signals that the exeuction should suspend,
     * by using the original continuation.
     */
    public static final String MODE_CANCEL = "cancel";
}
