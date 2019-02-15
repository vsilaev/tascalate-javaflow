/**
 * ﻿Copyright 2013-2019 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.core;

import java.lang.reflect.Proxy;

/**
 * Generic interface for continuable dynamic proxy classes.
 * Unlike standard Java {@link Proxy} or CGLib Proxy where it's possible to resolve proxy -&gt; handler 
 * dependency via API, custom continuable proxies should provide implementation of this interface to get 
 * corresponding "handler" from the proxied object, in the same manner as {@link Proxy#getInvocationHandler(Object)}. 
 * 
 * @author vsilaev
 *
 */
public interface CustomContinuableProxy {
    /**
     * Get a real continuable invocation handler for the proxy method descrived via <code>methodName</code>
     * and <code>methodDescription</code>.
     * 
     * @param methodName name of the continuable method that is proxied
     * @param methodDescription arguments/return type of the method in internal JVM format
     * @return the continuable handler that processed invocation 
     */
    public Object getInvocationHandler(String methodName, String methodDescription);
}
