/**
 * ﻿Copyright 2013-2021 Valery Silaev (http://vsilaev.com)
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
module net.tascalate.javaflow.providers.proxy {
    requires org.slf4j;

    requires net.tascalate.asmx;
    requires net.tascalate.asmx.plus;
    requires net.tascalate.asmx.commons;
    
    requires transitive net.tascalate.javaflow.providers.core;
    
    exports org.apache.commons.javaflow.providers.proxy;

}