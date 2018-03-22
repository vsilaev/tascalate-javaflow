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

import org.apache.commons.javaflow.api.Continuation;

public class CancelExample {

    public static void main(String[] argv) throws Exception {
        Continuation cc = Continuation.startSuspendedWith(new Execution());
        cc = cc.resume();
        System.out.println("In main, first stop, let's loop (cc.value = " + cc.value() + ") ");
        for (int i = 1; i <= 3; i++) {
            cc = cc.resume();
            System.out.println("In main after #" + i + " suspend (cc.value = " + cc.value() + ") ");
        }
        // This will gracefully complete continuation -- finally blocks will be
        // executed
        cc.terminate();
        System.out.println("In main after destroy");
        System.out.println("===");
    }

}
