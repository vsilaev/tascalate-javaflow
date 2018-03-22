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
package org.apache.commons.javaflow.examples.inheritance;

import org.apache.commons.javaflow.api.ccs;
import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.api.Continuation;

public class Execution implements Runnable {

    public @continuable void run() {
        // Single abstract method of anonymous class inside @Continuable method
        // Direct invocation over concrete type
        new Runnable() {
            @Override
            public @continuable void run() {
                System.out.println("Anonymous class -- before");
                Continuation.suspend("SAM Interface");
                System.out.println("Anonymous class -- after");
            }
        }.run();

        // Invocation over non-continuable supertype/interface
        // Notice that you MUST annotate variable
        @ccs
        Runnable x = new Runnable() {
            @Override
            public @continuable void run() {
                System.out.println("Ref Anonymous class -- before");
                Continuation.suspend("Ref SAM Interface");
                System.out.println("Ref Anonymous class -- after");
            }
        };
        x.run();

        @ccs
        IDemo d1 = createDemo();
        d1.call(11);

        DemoConcrete d2 = (DemoConcrete) d1;
        d2.call(77);

        try {
            Integer.valueOf("XYZ");
        } catch (NumberFormatException ex) {
            String message1 = "In ";
            String message2 = "exception ";
            String message3 = "handler";
            System.out.println(message1 + message2 + message3);
            x.run();
        }

    }

    @continuable
    void suspend(int i) {
        System.out.println("Exe before suspend");
        Continuation.suspend(i);
        System.out.println("Exe after suspend");
    }

    private IDemo createDemo() {
        DemoConcrete result = new DemoConcrete();
        result.outer = this;
        return result;
    }

}
