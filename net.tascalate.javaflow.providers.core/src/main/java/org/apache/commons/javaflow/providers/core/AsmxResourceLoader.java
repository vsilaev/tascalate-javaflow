/**
 * ﻿Copyright 2013-2022 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.providers.core;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.javaflow.spi.ResourceLoader;

class AsmxResourceLoader implements net.tascalate.asmx.plus.ResourceLoader {
    
    final ResourceLoader resourceLoader;
    
    AsmxResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public boolean hasResource(String name) {
        return resourceLoader.hasResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) throws IOException {
        return resourceLoader.getResourceAsStream(name);
    }

}
