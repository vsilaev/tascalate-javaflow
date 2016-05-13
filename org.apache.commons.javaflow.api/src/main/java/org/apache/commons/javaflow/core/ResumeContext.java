package org.apache.commons.javaflow.core;

public class ResumeContext {
	final private Error error;
	final private Object value;
	
	private ResumeContext(final Object value, final Error error) {
		this.value = value;
		this.error = error;
	}
	
	public static ResumeContext resumeWithValue(final Object value) {
		return new ResumeContext(value, null);
	}
	
	public static ResumeContext resumeWithError(final Error error) {
		return new ResumeContext(null, error);
	}

	
	public Object value() {
		return value;
	}
	
	void checkError() {
		if (null != error) {
			throw error;
		}
	}
}
