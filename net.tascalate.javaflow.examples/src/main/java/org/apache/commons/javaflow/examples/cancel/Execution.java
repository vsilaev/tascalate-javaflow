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
package org.apache.commons.javaflow.examples.cancel;

import java.util.Arrays;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

public class Execution implements Runnable {

    @Override
    public @continuable void run() {
        Object[] array = new String[] {"A", "C", "B"};
        try {
            int i = 1;
            System.out.println("Before suspend");
            Continuation.suspend("XYZ");
            System.out.println("After suspend #" + (i++) + ", should not mutate!!!");
            Continuation.cancel();
            // The line below will never be called --
            // first we are canceling continuation
            // then we are destroying it
            array[1] = "CHANGED";
        } finally {
            // This will be called only after cc.destroy() from outer code
            System.out.println("Finally is called, array value is " + Arrays.asList(array));
        }
    }
}
