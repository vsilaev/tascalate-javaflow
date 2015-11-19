package org.apache.commons.javaflow.instrumentation;

class StopException extends RuntimeException {

	final private static long serialVersionUID = 1L;

	@Override
	public Throwable fillInStackTrace() {
		return this;
	}

}
