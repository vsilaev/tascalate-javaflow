package org.apache.commons.javaflow.providers.proxy.owb;

import org.apache.commons.javaflow.providers.core.ContinuableClassInfo;

public class Owb4ProxyClassProcessor extends OwbProxyClassProcessor {

    public Owb4ProxyClassProcessor(int api, String className, ContinuableClassInfo classInfo) {
        super(api, className, classInfo);
    }

    @Override
    JEEClassLib jeeClassLib() {
        return JEEClassLib.JAKARTA;
    }
    
}
