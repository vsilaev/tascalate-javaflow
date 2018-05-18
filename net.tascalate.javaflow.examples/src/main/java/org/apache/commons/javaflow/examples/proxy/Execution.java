package org.apache.commons.javaflow.examples.proxy;

import org.apache.commons.javaflow.api.continuable;

public interface Execution {
    @continuable void execute();
    String nonContinuableTest();
}
