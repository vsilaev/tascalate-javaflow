package org.apache.commons.javaflow.examples.cdi.owb;

import org.apache.commons.javaflow.api.continuable;

interface TargetInterface {

    @continuable void execute(String prefix);

}
