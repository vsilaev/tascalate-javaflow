/**
 * ﻿Copyright 2013-2018 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.extras;

import java.io.Closeable;
import java.util.Iterator;

/**
 * Iterator that additionally implements {@link Closeable} interface 
 * to execute clean-up when not fully iterated
 * 
 * @author vsilaev
 *
 * @param <E>
 * Type of objects returned by the iterator 
 */
public interface CloseableIterator<E> extends Iterator<E>, AutoCloseable {
    @Override
    void close();
}
