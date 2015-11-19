package org.apache.commons.javaflow.instrumentation;

import java.util.Collections;
import java.util.Map;

class CurrentTarget {
	final String className;
	final byte[] classfileBuffer;
	
	CurrentTarget(final String className, final byte[] classfileBuffer) {
		this.className = className;
		this.classfileBuffer = classfileBuffer;
	}
	
	Map<String, byte[]> asResource() {
		return Collections.singletonMap(className + ".class", classfileBuffer);
	}
}
