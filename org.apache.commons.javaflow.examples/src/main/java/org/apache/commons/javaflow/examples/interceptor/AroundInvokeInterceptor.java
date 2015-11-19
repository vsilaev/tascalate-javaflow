package org.apache.commons.javaflow.examples.interceptor;

public class AroundInvokeInterceptor implements InterceptorInterface {
	
	final private TargetInterface target;
	
	public AroundInvokeInterceptor(TargetInterface target) {
		this.target = target; 
	}
	
	public void decorateCall(String param) {
		System.out.println("Around invoke before call");
       	target.execute(param);
       	System.out.println("Around invoke after call");
	}
}
