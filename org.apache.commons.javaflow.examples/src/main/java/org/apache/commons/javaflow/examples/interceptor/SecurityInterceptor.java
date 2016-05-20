package org.apache.commons.javaflow.examples.interceptor;

public class SecurityInterceptor implements InterceptorInterface {
	
	final private TargetInterface target;
	
	public SecurityInterceptor(TargetInterface target) {
		this.target = target; 
	}
	
	public void decorateCall(String param) {
		System.out.println("Security Interceptor before call");
       	target.execute(param);
       	System.out.println("Security Interceptor after call");
	}
}
