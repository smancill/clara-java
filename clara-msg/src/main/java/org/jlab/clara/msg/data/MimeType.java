/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.data;

/**
 * Predefined supported types.
 * <p>
 * Primitive and arrays of primitives will be stored inside a
 * {@link org.jlab.clara.msg.core.Message Message} as part of a protocol buffers
 * {@link org.jlab.clara.msg.data.PlainDataProto.PlainData PlainData} object.
 * Language specific types will be serialized and stored as a byte-array
 * (serialization for these types only work on actors of the same language).
 *
 * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding">
 *      Protocol Buffers Encoding</a>
 */
public final class MimeType {

    /**
     * A signed integer of 32 bytes and variable length.
     */
    public static final String SINT32 = "binary/sint32";

    /**
     * A signed integer of 64 bytes and variable length.
     */
    public static final String SINT64 = "binary/sint64";

    /**
     * A signed integer of 32 bytes and fixed length.
     */
    public static final String SFIXED32 = "binary/sfixed32";

    /**
     * A signed integer of 32 bytes and fixed length.
     */
    public static final String SFIXED64 = "binary/sfixed64";

    /**
     * A 32 bytes single-precision floating-point number.
     */
    public static final String FLOAT = "binary/float";

    /**
     * A 64 bytes single-precision floating-point number.
     */
    public static final String DOUBLE = "binary/double";

    /**
     * A string.
     */
    public static final String STRING = "text/string";

    /**
     * Binary data.
     */
    public static final String BYTES = "binary/bytes";


    /**
     * An array of signed integers of 32 bytes and variable length.
     */
    public static final String ARRAY_SINT32 = "binary/array-sint32";

    /**
     * An array of signed integers of 64 bytes and variable length.
     */
    public static final String ARRAY_SINT64 = "binary/array-sint64";

    /**
     * An array of signed integers of 32 bytes and fixed length.
     */
    public static final String ARRAY_SFIXED32 = "binary/array-sfixed32";

    /**
     * An array of signed integers of 32 bytes and fixed length.
     */
    public static final String ARRAY_SFIXED64 = "binary/array-sfixed64";

    /**
     * An array of 32 bytes single-precision floating-point numbers.
     */
    public static final String ARRAY_FLOAT = "binary/array-float";

    /**
     * An array of 64 bytes single-precision floating-point numbers.
     */
    public static final String ARRAY_DOUBLE = "binary/array-double";

    /**
     * An array of strings.
     */
    public static final String ARRAY_STRING = "binary/array-string";

    /**
     * A array of binary data.
     */
    public static final String ARRAY_BYTES = "binary/array-bytes";

    /**
     * A {@link org.jlab.clara.msg.data.PlainDataProto.PlainData PlainData} object.
     */
    public static final String PLAIN_DATA = "binary/clara-plain";

    /**
     * A Java object.
     */
    public static final String JOBJECT = "binary/java";

    /**
     * A C++ object.
     */
    public static final String COBJECT = "binary/cpp";

    /**
     * A Python object.
     */
    public static final String POBJECT = "binary/python";

    private MimeType() { }
}
