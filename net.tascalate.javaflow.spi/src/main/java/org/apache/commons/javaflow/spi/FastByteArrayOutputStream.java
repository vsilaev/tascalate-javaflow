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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class FastByteArrayOutputStream extends OutputStream {

    /**
     * The buffer where data is stored.
     */
    private byte buf[];

    /**
     * The number of valid bytes in the buffer.
     */
    private int count;

    /**
     * Creates a new byte array output stream. The buffer capacity is
     * initially 32 bytes, though its size increases if necessary.
     */
    public FastByteArrayOutputStream() {
        this(32);
    }

    /**
     * Creates a new byte array output stream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param   size   the initial size.
     * @exception  IllegalArgumentException if size is negative.
     */
    public FastByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: " + size);
        }
        buf = new byte[size];
    }

    public void write(int b) {
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }

    public void write(byte b[], int off, int len) {
        if ((off < 0)        || 
            (off > b.length) || 
            (len < 0)        ||
            ((off + len) - b.length > 0)) {
            
            throw new IndexOutOfBoundsException();
        }
        ensureCapacity(count + len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    /**
     * Writes the complete contents of this byte array output stream to
     * the specified output stream argument, as if by calling the output
     * stream's write method using <code>out.write(buf, 0, count)</code>.
     *
     * @param      out   the output stream to which to write the data.
     * @exception  IOException  if an I/O error occurs.
     */
    public void writeTo(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    /**
     * Resets the <code>count</code> field of this byte array output
     * stream to zero, so that all currently accumulated output in the
     * output stream is discarded. The output stream can be used again,
     * reusing the already allocated buffer space.
     */
    public void reset() {
        count = 0;
    }

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.
     *
     * @return  the current contents of this output stream, as a byte array.
     */    
    public byte[] toByteArray() {
        byte[] result = new byte[count];
        System.arraycopy(buf, 0, result, 0, count);
        return result;
    }

    public byte[] unsafeBytes() {
        if (count == buf.length) {
            return buf;
        } else {
            return toByteArray();
        }
    }

    /**
     * Returns the current size of the buffer.
     *
     * @return  the value of the <code>count</code> field, which is the number
     *          of valid bytes in this output stream.
     */
    public int size() {
        return count;
    }

    /**
     * Converts the buffer's contents into a string decoding bytes using the
     * platform's default character set. The length of the new <code>String</code>
     * is a function of the character set, and hence may not be equal to the
     * size of the buffer.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with the default replacement string for the platform's
     * default character set. The {@linkplain java.nio.charset.CharsetDecoder}
     * class should be used when more control over the decoding process is
     * required.
     *
     * @return String decoded from the buffer's contents.
     * 
     */    
    @Override
    public String toString() {
        return new String(buf, 0, count);
    }

    /**
     * Converts the buffer's contents into a string by decoding the bytes using
     * the named {@link java.nio.charset.Charset charset}. The length of the new
     * <code>String</code> is a function of the charset, and hence may not be equal
     * to the length of the byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string. The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param      charsetName  the name of a supported
     *             {@link java.nio.charset.Charset charset}
     * @return     String decoded from the buffer's contents.
     * @exception  UnsupportedEncodingException
     *             If the named charset is not supported
     */    
    public String toString(String charsetName) throws UnsupportedEncodingException {
        return new String(buf, 0, count, charsetName);
    }

    @Override
    public void close() {
    }
    

    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buf.length > 0)
            grow(minCapacity);
    }
    
    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        
        byte[] newBuf = new byte[newCapacity];
        System.arraycopy(buf, 0, newBuf, 0, count);
        buf = newBuf;
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }
    
    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

}
