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
package org.apache.commons.javaflow.examples.interceptor;

import org.apache.commons.javaflow.api.InterceptorSupport;
import org.apache.commons.javaflow.api.ccs;
import org.apache.commons.javaflow.api.continuable;

public class Execution implements Runnable {

    public @continuable void run() {
        TargetInterface target = new TargetClass();
        // Need either @ccs on var or @continuable type
        @ccs
        InterceptorInterface interceptor = new InterceptorGuard(
            new TransactionalInterceptor(new SecurityInterceptor(target))
        );
        String[] array = new String[] { "A", "B", "C" };
        for (int i = 0; i < array.length; i++) {
            System.out.println("Execution " + i);
            interceptor.decorateCall(array[i]);
        }
    }

    // Guard is required to balance stack variables
    static class InterceptorGuard implements InterceptorInterface {
        final InterceptorInterface next;

        public InterceptorGuard(InterceptorInterface next) {
            this.next = next;
        }

        public void decorateCall(String param) {
            InterceptorSupport.beforeExecution(null);
            try {
                // If there were no interceptors then we will have the following
                // call here:
                // TargetClass.execute(String) - this is what we pop-out above
                // with InterceptorSupport.beforeExecution();
                next.decorateCall(param);
            } finally {
                InterceptorSupport.afterExecution(this);
            }
        }
    }
}
