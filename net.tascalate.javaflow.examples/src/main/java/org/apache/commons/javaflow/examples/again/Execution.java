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
package org.apache.commons.javaflow.examples.again;

import java.security.SecureRandom;
import java.util.Random;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

public class Execution implements Runnable {

    @Override
    public @continuable void run() {
        Random rnd = new SecureRandom();
        try {
            Continuation.suspend();
            // LOOP_START
            System.out.println("resumed");

            int r = rnd.nextInt(5);
            if (r != 0) {
                System.out.println("do it again, r=" + r);
                Continuation.again(); // like "GOTO LOOP_START", first statement
                                      // after closest suspend()
            }

            System.out.println("done");
        } finally {
            // This will be called only once
            System.out.println("Finally is called");
        }
    }
}
