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
package org.apache.commons.javaflow.examples.proxy;

import java.lang.reflect.Proxy;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

public class ProxyExample {

    public static void main(String[] args) {
        Execution execution = (Execution) Proxy.newProxyInstance(
            ProxyExample.class.getClassLoader(), 
            new Class<?>[]{Execution.class}, 
            new LoggingInvocationHandler(new TargetClass()));
        
        System.out.println(execution);
        System.out.println(">>Non-continuable outside continuation: " + execution.nonContinuableTest());
        System.out.println("======");
        
        Runnable runnable = new Invoker(execution);
        for (Continuation cc = Continuation.startWith(runnable); null != cc;) {
            final String valueFromContinuation = String.class.cast(cc.value());
            System.out.println(">>Interrupted " + valueFromContinuation);
            // Let's continuation resume
            cc = cc.resume("processed-" + valueFromContinuation);
        }

        System.out.println("ALL DONE");

    }

    static class Invoker implements Runnable {
        final private Execution execution;
        
        Invoker(Execution execution) {
            this.execution = execution;
        }
        
        public @continuable void run() {
            System.out.println(">>Non-continuable inside continuation: " + execution.nonContinuableTest());
            execution.execute();
        }
    }
}
