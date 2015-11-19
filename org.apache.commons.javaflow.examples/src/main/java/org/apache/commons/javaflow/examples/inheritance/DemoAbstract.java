package org.apache.commons.javaflow.examples.inheritance;

import org.apache.commons.javaflow.api.continuable;

@continuable
abstract public class DemoAbstract implements IDemo {
	Execution outer;
}
