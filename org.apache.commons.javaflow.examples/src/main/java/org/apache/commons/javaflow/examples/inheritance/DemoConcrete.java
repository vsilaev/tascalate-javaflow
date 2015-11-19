package org.apache.commons.javaflow.examples.inheritance;

import org.apache.commons.javaflow.api.continuable;

//@Continuable -- inherited via DemoAbstract
public class DemoConcrete extends DemoAbstract {

	@continuable // should declare while parents do not 
	public void call(int payload) {
		outer.suspend(payload);
		System.out.println("DemoConcrete done");
	}
}
