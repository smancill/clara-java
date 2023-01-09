/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.data;

/**
 * Predefined supported types.
 * <p>
 * Primitives will be stored inside a
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
     * An integer of 32 bytes.
     */
    public static final String INT32 = "binary/int32";

    /**
     * An integer of 64 bytes.
     */
    public static final String INT64 = "binary/int64";

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
