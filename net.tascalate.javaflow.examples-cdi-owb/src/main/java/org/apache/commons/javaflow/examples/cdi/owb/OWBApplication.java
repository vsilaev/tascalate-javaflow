/**
 * ﻿Copyright 2013-2017 Valery Silaev (http://vsilaev.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    
    @Inject
    SimpleInterface simpleInterface;

    public void run() {
        int i = 0;
        for (Continuation cc = Continuation.startWith(execution); null != cc; cc = cc.resume(i += 100)) {
            System.out.println("SUSPENDED " + cc.value());
        }

        System.out.println("===");
        simpleInterface.execute("ABC");
        ((SimpleBean)simpleInterface).executeNested("XYZ");
    }

    public static void main(String[] argv) throws Exception {
        boot(null);
        try {
            BeanManager beanManager = lifecycle.getBeanManager();
            Bean<?> bean = beanManager.getBeans(OWBApplication.class).iterator().next();
            OWBApplication app = (OWBApplication) lifecycle.getBeanManager().getReference(
                bean, OWBApplication.class, beanManager.createCreationalContext(bean)
            );
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
