package org.apache.commons.javaflow.examples.cdi.owb;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContainerLifecycle;

public class OWBApplication {

    @Inject 
    Execution execution;

    public void run() {
        int i = 0;
        for (Continuation cc = Continuation.startWith(execution); null != cc; cc = cc.resume(i += 100)) {
            System.out.println("SUSPENDED " + cc.value());
        }
        
        System.out.println("===");
    }
    
    public static void main(final String[] argv) throws Exception {
        boot(null);
        try {
            BeanManager beanManager = lifecycle.getBeanManager();
            Bean<?> bean = beanManager.getBeans(OWBApplication.class).iterator().next();
            OWBApplication app = (OWBApplication)lifecycle.getBeanManager().getReference(bean, OWBApplication.class, beanManager.createCreationalContext(bean));
            app.run();
        } finally {
            shutdown(null);
        }
    }
    
    private static ContainerLifecycle lifecycle = null;
    
    private static void boot(Object startupObject) throws Exception {
        lifecycle = WebBeansContext.getInstance().getService(ContainerLifecycle.class);
        lifecycle.startApplication(startupObject);
    }
    
    private static void shutdown(Object endObject) throws Exception {
        lifecycle.stopApplication(endObject);
    }
}
