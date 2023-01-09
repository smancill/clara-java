/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.data;

/**
 * Predefined supported types.
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

    private MimeType() { }
}
