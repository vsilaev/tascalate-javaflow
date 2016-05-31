package org.apache.commons.javaflow.core;

public class SuspendResult {

	public final static SuspendResult CANCEL = new SuspendResult();
	
	public final static SuspendResult AGAIN = new SuspendResult();
	
	public final static SuspendResult EXIT = new SuspendResult();
	
	private final static SuspendResult NULL_VALUE = new SuspendResult() {
		public Object value() {
			return null;
		}
	};
	
	public Object value() {
		throw new UnsupportedOperationException();
	}
	
	public static SuspendResult valueOf(final Object value) {
		return null == value ? NULL_VALUE : new SuspendResult() {
			public Object value() {
				return value;
			}
		};
	}
	
	
}
