// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.apache.commons.javaflow.spi;

/**
 * Class header reader -- like class name, superclass, interfaces. Based on an
 * OW2 ASM ClassReadercthat reads class structure, as defined in the Java
 * Virtual Machine Specification (JVMS). encountered.
 *
 * @see <a href=
 *      "https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html">JVMS
 *      4</a>
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
class ClassHeaderReader {
    /**
     * A byte array containing the JVMS ClassFile structure to be parsed. <i>The
     * content of this array must not be modified. This field is intended for
     * Attribute sub classes, and is normally not needed by class
     * visitors.</i>
     *
     * <p>
     * NOTE: the ClassFile structure can start at any offset within this array, i.e.
     * it does not necessarily start at offset 0. Use {@link #getItem} and
     * {@link #header} to get correct ClassFile element offsets within this byte
     * array.
     */
    // DontCheck(MemberName): can't be renamed (for backward binary compatibility).
    private final byte[] b;

    /**
     * The offset in bytes, in {@link #b}, of each cp_info entry of the ClassFile's
     * constant_pool array, <i>plus one</i>. In other words, the offset of constant
     * pool entry i is given by cpInfoOffsets[i] - 1, i.e. its cp_info's tag field
     * is given by b[cpInfoOffsets[i] - 1].
     */
    private final int[] cpInfoOffsets;

    /**
     * The String objects corresponding to the CONSTANT_Utf8 constant pool items.
     * This cache avoids multiple parsing of a given CONSTANT_Utf8 constant pool
     * item.
     */
    private final String[] constantUtf8Values;

    /**
     * A conservative estimate of the maximum length of the strings contained in the
     * constant pool of the class.
     */
    private final int maxStringLength;

    /**
     * The offset in bytes, in {@link #b}, of the ClassFile's access_flags field.
     */
    private final int header;

    // -----------------------------------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------------------------------

    /**
     * Constructs a new {@link ClassHeaderReader} object.
     *
     * @param classFile
     *            the JVMS ClassFile structure to be read.
     */
    ClassHeaderReader(final byte[] classFile) {
        this(classFile, 0);
    }

    /**
     * Constructs a new {@link ClassHeaderReader} object. <i>This internal constructor
     * must not be exposed as a public API</i>.
     *
     * @param classFileBuffer
     *            a byte array containing the JVMS ClassFile structure to be read.
     * @param classFileOffset
     *            the offset in byteBuffer of the first byte of the ClassFile to be
     *            read.
     * @param checkClassVersion
     *            whether to check the class version or not.
     */
    private ClassHeaderReader(final byte[] classFileBuffer, final int classFileOffset) {
        b = classFileBuffer;
        /*
        int V18 = 0 << 16 | 62;
        // Check the class' major_version. This field is after the magic and
        // minor_version fields, which
        // use 4 and 2 bytes respectively.
        if (checkClassVersion && readShort(classFileOffset + 6) > V18) {
            throw new IllegalArgumentException(
                    "Unsupported class file major version " + readShort(classFileOffset + 6));
        }
        */
        // Create the constant pool arrays. The constant_pool_count field is after the
        // magic,
        // minor_version and major_version fields, which use 4, 2 and 2 bytes
        // respectively.
        int constantPoolCount = readUnsignedShort(classFileOffset + 8);
        cpInfoOffsets = new int[constantPoolCount];
        constantUtf8Values = new String[constantPoolCount];
        // Compute the offset of each constant pool entry, as well as a conservative
        // estimate of the
        // maximum length of the constant pool strings. The first constant pool entry is
        // after the
        // magic, minor_version, major_version and constant_pool_count fields, which use
        // 4, 2, 2 and 2
        // bytes respectively.
        int currentCpInfoIndex = 1;
        int currentCpInfoOffset = classFileOffset + 10;
        int currentMaxStringLength = 0;
        @SuppressWarnings("unused")
        boolean hasConstantDynamic = false;
        // The offset of the other entries depend on the total size of all the
        // previous entries.        
        @SuppressWarnings("unused")
        boolean hasBootstrapMethods = false;
        while (currentCpInfoIndex < constantPoolCount) {
            cpInfoOffsets[currentCpInfoIndex++] = currentCpInfoOffset + 1;
            int cpInfoSize;
            switch (classFileBuffer[currentCpInfoOffset]) {
            case Symbol.CONSTANT_FIELDREF_TAG:
            case Symbol.CONSTANT_METHODREF_TAG:
            case Symbol.CONSTANT_INTERFACE_METHODREF_TAG:
            case Symbol.CONSTANT_INTEGER_TAG:
            case Symbol.CONSTANT_FLOAT_TAG:
            case Symbol.CONSTANT_NAME_AND_TYPE_TAG:
                cpInfoSize = 5;
                break;
            case Symbol.CONSTANT_DYNAMIC_TAG:
                cpInfoSize = 5;
                hasBootstrapMethods = true;
                hasConstantDynamic = true;
                break;
            case Symbol.CONSTANT_INVOKE_DYNAMIC_TAG:
                cpInfoSize = 5;
                hasBootstrapMethods = true;
                break;
            case Symbol.CONSTANT_LONG_TAG:
            case Symbol.CONSTANT_DOUBLE_TAG:
                cpInfoSize = 9;
                currentCpInfoIndex++;
                break;
            case Symbol.CONSTANT_UTF8_TAG:
                cpInfoSize = 3 + readUnsignedShort(currentCpInfoOffset + 1);
                if (cpInfoSize > currentMaxStringLength) {
                    // The size in bytes of this CONSTANT_Utf8 structure provides a conservative
                    // estimate
                    // of the length in characters of the corresponding string, and is much cheaper
                    // to
                    // compute than this exact length.
                    currentMaxStringLength = cpInfoSize;
                }
                break;
            case Symbol.CONSTANT_METHOD_HANDLE_TAG:
                cpInfoSize = 4;
                break;
            case Symbol.CONSTANT_CLASS_TAG:
            case Symbol.CONSTANT_STRING_TAG:
            case Symbol.CONSTANT_METHOD_TYPE_TAG:
            case Symbol.CONSTANT_PACKAGE_TAG:
            case Symbol.CONSTANT_MODULE_TAG:
                cpInfoSize = 3;
                break;
            default:
                throw new IllegalArgumentException();
            }
            currentCpInfoOffset += cpInfoSize;
        }
        maxStringLength = currentMaxStringLength;
        // The Classfile's access_flags field is just after the last constant pool
        // entry.
        header = currentCpInfoOffset;
    }
    // -----------------------------------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------------------------------

    /**
     * Returns the internal name of the class.
     *
     * @return the internal class name.
     */
    String getClassName() {
        // this_class is just after the access_flags field (using 2 bytes).
        return readClass(header + 2, new char[maxStringLength]);
    }

    // -----------------------------------------------------------------------------------------------
    // Public methods
    // -----------------------------------------------------------------------------------------------

    // -----------------------------------------------------------------------------------------------
    // Utility methods: low level parsing
    // -----------------------------------------------------------------------------------------------
    /**
     * Reads an unsigned short value in {@link #b}. <i>This method is intended for
     * Attribute sub classes, and is normally not needed by class generators
     * or adapters.</i>
     *
     * @param offset
     *            the start index of the value to be read in {@link #b}.
     * @return the read value.
     */
    private int readUnsignedShort(final int offset) {
        byte[] classFileBuffer = b;
        return ((classFileBuffer[offset] & 0xFF) << 8) | (classFileBuffer[offset + 1] & 0xFF);
    }

    /**
     * Reads a signed short value in {@link #b}. <i>This method is intended for
     * Attribute sub classes, and is normally not needed by class generators
     * or adapters.</i>
     *
     * @param offset
     *            the start offset of the value to be read in {@link #b}.
     * @return the read value.
     */
    @SuppressWarnings("unused")
    private short readShort(final int offset) {
        byte[] classFileBuffer = b;
        return (short) (((classFileBuffer[offset] & 0xFF) << 8) | (classFileBuffer[offset + 1] & 0xFF));
    }

    
    /**
     * Reads a CONSTANT_Utf8 constant pool entry in {@link #b}. <i>This method is
     * intended for Attribute sub classes, and is normally not needed by
     * class generators or adapters.</i>
     *
     * @param offset
     *            the start offset of an unsigned short value in {@link #b}, whose
     *            value is the index of a CONSTANT_Utf8 entry in the class's
     *            constant pool table.
     * @param charBuffer
     *            the buffer to be used to read the string. This buffer must be
     *            sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified CONSTANT_Utf8 entry.
     */
    // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary
    // compatibility).
    private String readUTF8(final int offset, final char[] charBuffer) {
        int constantPoolEntryIndex = readUnsignedShort(offset);
        if (offset == 0 || constantPoolEntryIndex == 0) {
            return null;
        }
        return readUtf(constantPoolEntryIndex, charBuffer);
    }

    /**
     * Reads a CONSTANT_Utf8 constant pool entry in {@link #b}.
     *
     * @param constantPoolEntryIndex
     *            the index of a CONSTANT_Utf8 entry in the class's constant pool
     *            table.
     * @param charBuffer
     *            the buffer to be used to read the string. This buffer must be
     *            sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified CONSTANT_Utf8 entry.
     */
    final private String readUtf(final int constantPoolEntryIndex, final char[] charBuffer) {
        String value = constantUtf8Values[constantPoolEntryIndex];
        if (value != null) {
            return value;
        }
        int cpInfoOffset = cpInfoOffsets[constantPoolEntryIndex];
        return constantUtf8Values[constantPoolEntryIndex] = readUtf(cpInfoOffset + 2, readUnsignedShort(cpInfoOffset),
                charBuffer);
    }

    /**
     * Reads an UTF8 string in {@link #b}.
     *
     * @param utfOffset
     *            the start offset of the UTF8 string to be read.
     * @param utfLength
     *            the length of the UTF8 string to be read.
     * @param charBuffer
     *            the buffer to be used to read the string. This buffer must be
     *            sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified UTF8 string.
     */
    private String readUtf(final int utfOffset, final int utfLength, final char[] charBuffer) {
        int currentOffset = utfOffset;
        int endOffset = currentOffset + utfLength;
        int strLength = 0;
        byte[] classFileBuffer = b;
        while (currentOffset < endOffset) {
            int currentByte = classFileBuffer[currentOffset++];
            if ((currentByte & 0x80) == 0) {
                charBuffer[strLength++] = (char) (currentByte & 0x7F);
            } else if ((currentByte & 0xE0) == 0xC0) {
                charBuffer[strLength++] = (char) (((currentByte & 0x1F) << 6)
                        + (classFileBuffer[currentOffset++] & 0x3F));
            } else {
                charBuffer[strLength++] = (char) (((currentByte & 0xF) << 12)
                        + ((classFileBuffer[currentOffset++] & 0x3F) << 6) + (classFileBuffer[currentOffset++] & 0x3F));
            }
        }
        return new String(charBuffer, 0, strLength);
    }

    /**
     * Reads a CONSTANT_Class, CONSTANT_String, CONSTANT_MethodType, CONSTANT_Module
     * or CONSTANT_Package constant pool entry in {@link #b}. <i>This method is
     * intended for Attribute sub classes, and is normally not needed by
     * class generators or adapters.</i>
     *
     * @param offset
     *            the start offset of an unsigned short value in {@link #b}, whose
     *            value is the index of a CONSTANT_Class, CONSTANT_String,
     *            CONSTANT_MethodType, CONSTANT_Module or CONSTANT_Package entry in
     *            class's constant pool table.
     * @param charBuffer
     *            the buffer to be used to read the item. This buffer must be
     *            sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified constant pool entry.
     */
    private String readStringish(final int offset, final char[] charBuffer) {
        // Get the start offset of the cp_info structure (plus one), and read the
        // CONSTANT_Utf8 entry
        // designated by the first two bytes of this cp_info.
        return readUTF8(cpInfoOffsets[readUnsignedShort(offset)], charBuffer);
    }

    /**
     * Reads a CONSTANT_Class constant pool entry in {@link #b}. <i>This method is
     * intended for Attribute sub classes, and is normally not needed by
     * class generators or adapters.</i>
     *
     * @param offset
     *            the start offset of an unsigned short value in {@link #b}, whose
     *            value is the index of a CONSTANT_Class entry in class's constant
     *            pool table.
     * @param charBuffer
     *            the buffer to be used to read the item. This buffer must be
     *            sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified CONSTANT_Class entry.
     */
    private String readClass(final int offset, final char[] charBuffer) {
        return readStringish(offset, charBuffer);
    }

    private static class Symbol {

        // Tag values for the constant pool entries (using the same order as in the
        // JVMS).

        /** The tag value of CONSTANT_Class_info JVMS structures. */
        static final int CONSTANT_CLASS_TAG = 7;

        /** The tag value of CONSTANT_Fieldref_info JVMS structures. */
        static final int CONSTANT_FIELDREF_TAG = 9;

        /** The tag value of CONSTANT_Methodref_info JVMS structures. */
        static final int CONSTANT_METHODREF_TAG = 10;

        /** The tag value of CONSTANT_InterfaceMethodref_info JVMS structures. */
        static final int CONSTANT_INTERFACE_METHODREF_TAG = 11;

        /** The tag value of CONSTANT_String_info JVMS structures. */
        static final int CONSTANT_STRING_TAG = 8;

        /** The tag value of CONSTANT_Integer_info JVMS structures. */
        static final int CONSTANT_INTEGER_TAG = 3;

        /** The tag value of CONSTANT_Float_info JVMS structures. */
        static final int CONSTANT_FLOAT_TAG = 4;

        /** The tag value of CONSTANT_Long_info JVMS structures. */
        static final int CONSTANT_LONG_TAG = 5;

        /** The tag value of CONSTANT_Double_info JVMS structures. */
        static final int CONSTANT_DOUBLE_TAG = 6;

        /** The tag value of CONSTANT_NameAndType_info JVMS structures. */
        static final int CONSTANT_NAME_AND_TYPE_TAG = 12;

        /** The tag value of CONSTANT_Utf8_info JVMS structures. */
        static final int CONSTANT_UTF8_TAG = 1;

        /** The tag value of CONSTANT_MethodHandle_info JVMS structures. */
        static final int CONSTANT_METHOD_HANDLE_TAG = 15;

        /** The tag value of CONSTANT_MethodType_info JVMS structures. */
        static final int CONSTANT_METHOD_TYPE_TAG = 16;

        /** The tag value of CONSTANT_Dynamic_info JVMS structures. */
        static final int CONSTANT_DYNAMIC_TAG = 17;

        /** The tag value of CONSTANT_InvokeDynamic_info JVMS structures. */
        static final int CONSTANT_INVOKE_DYNAMIC_TAG = 18;

        /** The tag value of CONSTANT_Module_info JVMS structures. */
        static final int CONSTANT_MODULE_TAG = 19;

        /** The tag value of CONSTANT_Package_info JVMS structures. */
        static final int CONSTANT_PACKAGE_TAG = 20;
    }
}