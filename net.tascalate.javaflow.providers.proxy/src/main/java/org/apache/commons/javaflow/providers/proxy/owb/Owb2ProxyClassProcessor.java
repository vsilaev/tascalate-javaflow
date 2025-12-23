package org.apache.commons.javaflow.providers.proxy.owb;

import org.apache.commons.javaflow.providers.core.ContinuableClassInfo;

public class Owb2ProxyClassProcessor extends OwbProxyClassProcessor {

    public Owb2ProxyClassProcessor(int api, String className, ContinuableClassInfo classInfo) {
        super(api, className, classInfo);
    }

    @Override
    JEEClassLib jeeClassLib() {
        return JEEClassLib.JAVAX;
    }
    
}
