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
package org.apache.commons.javaflow.spi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StopException extends RuntimeException {

    final private static long serialVersionUID = 1L;

    private StopException() { }
    
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    public static final StopException INSTANCE = new StopException();
    
    public static boolean __dirtyCheckSkipContinuationsOnClass(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (null != interfaces) {
            for (final String intf : interfaces){
                if (PROXY_MARKER_INTERFACES.contains(intf)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static final Set<String> PROXY_MARKER_INTERFACES = new HashSet<String>(Arrays.asList(
        "org/apache/webbeans/proxy/OwbInterceptorProxy", 
        "org/apache/webbeans/proxy/OwbNormalScopeProxy",
        "org/jboss/weld/bean/proxy/ProxyObject"
    )); 
}
