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
package org.apache.commons.javaflow.spi;

import java.io.InputStream;

public class FastByteArrayInputStream extends InputStream {

    /** The array to read from the input stream. */
    public byte[] array;

    /** The first valid index. */
    public int offset;

    /**
     * The number of valid bytes in {@link #array} starting from
     * {@link #offset}.
     */
    public int length;

    /** The current position as a distance from {@link #offset}. */
    private int position;

    /** The current mark as a position, or -1 if no mark exists. */
    private int mark;

    /**
     * Creates a new byte array input stream using a given array fragment.
     *
     * @param array
     *            the array to read from.
     * @param offset
     *            the first valid index of the array.
     * @param length
     *            the number of valid bytes.
     */
    public FastByteArrayInputStream(byte[] array, final int offset, final int length) {
        this.array = array;
        this.offset = offset;
        this.length = length;
    }

    /**
     * Creates a new byte array input stream using a given array.
     *
     * @param array
     *            the array to read from.
     */
    public FastByteArrayInputStream(byte[] array) {
        this(array, 0, array.length);
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void reset() {
        position = mark;
    }

    @Override
    public void close() {
    }

    @Override
    public void mark(int dummy) {
        mark = position;
    }

    @Override
    public int available() {
        return length - position;
    }

    @Override
    public long skip(long n) {
        if (n <= length - position) {
            position += (int) n;
            return n;
        }
        n = length - position;
        position = length;
        return n;
    }

    public int read() {
        if (length == position)
            return -1;
        return array[offset + position++] & 0xFF;
    }

    @Override
    public int read(byte b[], int offset, int length) {
        if (this.length == this.position)
            return length == 0 ? 0 : -1;
        
        int n = Math.min(length, this.length - this.position);
        
        System.arraycopy(array, this.offset + this.position, b, offset, n);
        
        this.position += n;
        return n;
    }
}
