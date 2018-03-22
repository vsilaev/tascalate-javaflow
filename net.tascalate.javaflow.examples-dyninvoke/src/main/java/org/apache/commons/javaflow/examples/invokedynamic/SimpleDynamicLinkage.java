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
package org.apache.commons.javaflow.examples.invokedynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.apache.commons.javaflow.api.continuable;
import org.apache.commons.javaflow.api.Continuation;

public class SimpleDynamicLinkage {

    @continuable
    static void sayHello() {
        System.out.println("There we go!");
        for (char c = 'A'; c < 'G'; c++) {
            Continuation.suspend("Data" + c);
        }
        System.out.println("CallSite continuation finished");
    }

    public static CallSite bootstrapDynamic(MethodHandles.Lookup caller, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> thisClass = lookup.lookupClass(); // (who am I?)
        MethodHandle sayHello = lookup.findStatic(thisClass, "sayHello", MethodType.methodType(void.class));
        return new ConstantCallSite(sayHello.asType(type));
    }

}