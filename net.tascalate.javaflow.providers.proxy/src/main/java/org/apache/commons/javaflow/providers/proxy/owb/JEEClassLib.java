package org.apache.commons.javaflow.providers.proxy.owb;

import net.tascalate.asmx.Type;

public enum JEEClassLib {
    JAVAX() {
        public Type providerType() {
            return JAVAX_PROVIDER_TYPE;
        } 
    },
    JAKARTA() {
        public Type providerType() {
            return JAKARTA_PROVIDER_TYPE;
        }
    };

    public abstract Type providerType();

    private static final Type JAVAX_PROVIDER_TYPE = Type.getObjectType("javax/inject/Provider");
    private static final Type JAKARTA_PROVIDER_TYPE = Type.getObjectType("jakarta/inject/Provider");
}
