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
package org.apache.commons.javaflow.examples.inner_outer;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

public class Execution implements Runnable {

    @Override
    public @continuable void run() {
        Inner inner = new Inner();
        for (int i = 1; i <= 5; i++) {
            inner.innerMethod(i);
        }
    }

    // Private, to show that accessor$### methods are instrumented
    private @continuable void outerMethod(int i) {
        System.out.println("Exe before suspend");
        Object fromCaller = Continuation.suspend(i);
        System.out.println("Exe after suspend: " + fromCaller);
    }

    class Inner {
        // Private, to show that accessor$### methods are instrumented
        private @continuable void innerMethod(int i) {
            outerMethod(i);
        }
    }
}
