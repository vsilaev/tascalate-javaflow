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
package org.apache.commons.javaflow.examples.lambdas;

import static org.apache.commons.javaflow.examples.lambdas.ContinuableAdapters.consumer;
import static org.apache.commons.javaflow.examples.lambdas.ContinuableAdapters.runnable;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.javaflow.api.ccs;
import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.extras.Continuations;

public class LambdasExample {
    public static void main(String[] argv) throws Exception {
        LambdasExample demo = new LambdasExample();
        Continuations.execute(demo::runExamples, s -> System.out.println("Interrupted " + s));

        System.out.println("===");

    }

    String ref = " ** ";

    private @continuable void runExamples() {

        // @Continuable method references and @Continuable SAM interfaces are
        // supported
        MyContinuableRunnable r1 = this::lambdaDemo;
        r1.run();

        MyContinuableRunnable r2 = () -> {
            System.out.println("ContinuableRunnable by arrow function -- before");
            Continuation.suspend(" ** ContinuableRunnable by arrow function");
            System.out.println("ContinuableRunnable by arrow function -- after");
        };
        r2.run();

        // Lambda reference MUST have annotated CallSite if SAM interface is not
        // @continuable
        // Notice that we still MUST create right interface
        // (MyContinuableRunnable in this case)
        @ccs
        Runnable closure = runnable(() -> {
            System.out.println("Plain Runnable Lambda by arrow function -- before");
            Continuation.suspend(" ** Plain Runnable Lambda by arrow function" + ref);
            System.out.println("Plain Runnable Lambda by arrow function -- after");
        });
        closure.run();

        // We can't use stream.forEach as is, but we can provide good enough
        // helpers
        // See org.apache.commons.javaflow.examples.lambdas.ContinuableAdapters
        // for
        // definition of "consumer" & "forEach"
        List<String> listOfStrings = Arrays.asList("A", null, "B", null, "C");
        Continuations.forEach(listOfStrings.stream().filter(s -> s != null).map(s -> s + s),
                consumer(this::yieldString1).andThen(this::yieldString2));

    }

    // Must be annotated to be used as lambda by method-ref
    @continuable void lambdaDemo() {
        System.out.println("Lambda by method reference -- before");
        Continuation.suspend(" ** Lambda by method reference** ");
        System.out.println("Lambda by method reference -- after");
    }

    @continuable void yieldString1(String s) {
        System.out.println("Before yield I " + s);
        Continuation.suspend("yield I " + s);
        System.out.println("After yield I " + s);
    }

    @continuable void yieldString2(String s) {
        System.out.println("Before yield II");
        Continuation.suspend("yield II " + s);
        System.out.println("After yield II");
    }

}
