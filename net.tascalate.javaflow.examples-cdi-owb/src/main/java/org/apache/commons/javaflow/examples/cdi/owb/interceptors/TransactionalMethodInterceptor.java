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
package org.apache.commons.javaflow.examples.cdi.owb.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.commons.javaflow.examples.cdi.owb.annotations.TransactionalMethod;

@TransactionalMethod @Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 1)
public class TransactionalMethodInterceptor {

    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Throwable {
        System.out.println("> Begin transaction... " + ctx.getMethod());
        boolean success = true;
        try {
            return ctx.proceed();
        } catch (Throwable ex) {
            System.out.println("> ...Rollback transaction " + ctx.getMethod());
            success = false;
            throw ex;
        } finally {
            if (success) {
                System.out.println("> ...Commit transaction " + ctx.getMethod());
            }
        }
    }
}
