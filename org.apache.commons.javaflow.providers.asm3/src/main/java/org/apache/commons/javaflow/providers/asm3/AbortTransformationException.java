package org.apache.commons.javaflow.providers.asm3;

class AbortTransformationException extends RuntimeException {

	final private static long serialVersionUID = 1L;

	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
	
	final static AbortTransformationException INSTANCE = new AbortTransformationException();
}
