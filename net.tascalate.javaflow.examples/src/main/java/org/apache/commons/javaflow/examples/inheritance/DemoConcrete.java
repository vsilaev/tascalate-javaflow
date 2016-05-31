package org.apache.commons.javaflow.examples.inheritance;

import org.apache.commons.javaflow.api.continuable;

public class DemoConcrete extends DemoAbstract {

	// should declare while parents do not 
	public @continuable void call(int payload) {
		outer.suspend(payload);
		System.out.println("DemoConcrete done");
	}
}
