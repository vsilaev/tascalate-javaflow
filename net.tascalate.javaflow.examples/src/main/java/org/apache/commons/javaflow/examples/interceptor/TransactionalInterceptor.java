package org.apache.commons.javaflow.examples.interceptor;

public class TransactionalInterceptor implements InterceptorInterface {
	
	final private InterceptorInterface target;
	
	public TransactionalInterceptor(InterceptorInterface target) {
		this.target = target; 
	}
	
	public void decorateCall(String param) {
		System.out.println("Transactional before call");
       	target.decorateCall(param);
       	System.out.println("Transactional after call");
	}
}
