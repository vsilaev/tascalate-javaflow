package org.apache.commons.javaflow.core;

public class ResumeParameter {
	final private Object value;
	
	private ResumeParameter(final Object value) {
		this.value = value;
	}
	
	public static ResumeParameter value(final Object value) {
		return null == value ? NULL_VALUE : new ResumeParameter(value);
	}
	
	public static ResumeParameter exit() {
		return EXIT;
	}

	final Object value() {
		return value;
	}
	
	void checkExit() {
		
	}
	
	private final static ResumeParameter NULL_VALUE = new ResumeParameter(null);
	
	private final static ResumeParameter EXIT = new ResumeParameter(null) {
		void checkExit() {
			throw ContinuationDeath.INSTANCE;
		}
	};
}
