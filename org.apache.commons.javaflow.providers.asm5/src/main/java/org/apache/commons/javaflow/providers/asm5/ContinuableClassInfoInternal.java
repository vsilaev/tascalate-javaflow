package org.apache.commons.javaflow.providers.asm5;

import java.util.Set;

import org.apache.commons.javaflow.spi.ContinuableClassInfo;

class ContinuableClassInfoInternal implements ContinuableClassInfo {
	private boolean processed;
	private Set<String> methods;

	public ContinuableClassInfoInternal(final boolean defaultProcessed, final Set<String> methods) {
		this.processed = defaultProcessed;
		this.methods = methods;
	}

	public boolean isClassProcessed() {
		return processed;
	}

	public void markClassProcessed() {
		processed = true;
	}
	
	public boolean isContinuableMethod(final int access, final String name, final String desc, final String signature) {
		return methods.contains(name + desc);
	}
	
	Set<String> continuableMethods() {
		return methods;
	}

}
