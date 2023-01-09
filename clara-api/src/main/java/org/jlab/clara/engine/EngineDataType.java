/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.engine;

import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import org.jlab.clara.base.error.ClaraException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Defines a data type used by a {@link Engine service engine}.
 * Data type can be a predefined type, or a custom-defined type.
 * When declaring a custom type, its serialization routine must be provided.
 */
public class EngineDataType {

    /**
     * An integer of 32 bits.
     */
    public static final EngineDataType INT32 = buildPrimitive(MimeType.INT32);

    /**
     * An integer of 64 bits.
     */
    public static final EngineDataType INT64 = buildPrimitive(MimeType.INT64);

    /**
     * A float (32 bits floating-point number).
     */
    public static final EngineDataType FLOAT = buildPrimitive(MimeType.FLOAT);

    /**
     * A double (64 bits floating-point number).
     */
    public static final EngineDataType DOUBLE = buildPrimitive(MimeType.DOUBLE);

    /**
     * A string.
     */
    public static final EngineDataType STRING = buildString(MimeType.STRING);

    /**
     * Raw bytes.
     * On Java a {@link ByteBuffer} is used to wrap the byte array and its endianness.
     */
    public static final EngineDataType BYTES = buildRawBytes();

    /**
     * JSON text.
     */
    public static final EngineDataType JSON = buildString(MimeType.JSON);

    private final String mimeType;
    private final ClaraSerializer serializer;

    /**
     * Creates a new user data type.
     * The data type is identified by its mime-type string.
     * The serializer will be used in order to send data through the network,
     * or to a different language DPE.
     *
     * @param mimeType the name of this data-type
     * @param serializer the custom serializer for this data-type
     */
    public EngineDataType(String mimeType, ClaraSerializer serializer) {
        Objects.requireNonNull(mimeType, "null mime-type");
        Objects.requireNonNull(serializer, "null serializer");
        if (mimeType.isEmpty()) {
            throw new IllegalArgumentException("empty mime-type");
        }
        this.mimeType = mimeType;
        this.serializer = serializer;
    }

    private static EngineDataType buildPrimitive(MimeType mimeType) {
        return new EngineDataType(mimeType.toString(), new PrimitiveSerializer(mimeType));
    }

    private static EngineDataType buildRawBytes() {
        return new EngineDataType(MimeType.BYTES.toString(), new RawBytesSerializer());
    }

    private static EngineDataType buildString(MimeType mimeType) {
        return new EngineDataType(mimeType.toString(), new StringSerializer());
    }


    /**
     * Returns the name of this data type.
     *
     * @return the mime-type string
     */
    public String mimeType() {
        return mimeType;
    }

    /**
     * Returns the serializer of this data type.
     *
     * @return the serializer object
     */
    public ClaraSerializer serializer() {
        return serializer;
    }

    @Override
    public String toString() {
        return mimeType;
    }


    // checkstyle.off: MethodParamPad
    private enum MimeType {
        INT32           ("binary/int32"),
        INT64           ("binary/int64"),
        FLOAT           ("binary/float"),
        DOUBLE          ("binary/double"),
        STRING          ("text/string"),
        BYTES           ("binary/bytes"),
        JSON            ("application/json");

        private final String name;

        MimeType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    // checkstyle.on: MethodParamPad


    private static class StringSerializer implements ClaraSerializer {

        @Override
        public ByteBuffer write(Object data) throws ClaraException {
            var text = (String) data;
            return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public Object read(ByteBuffer data) throws ClaraException {
            return new String(data.array(), StandardCharsets.UTF_8);
        }
    }


    private static class RawBytesSerializer implements ClaraSerializer {

        @Override
        public ByteBuffer write(Object data) throws ClaraException {
            return (ByteBuffer) data;
        }

        @Override
        public Object read(ByteBuffer data) throws ClaraException {
            return data;
        }
    }


    private static class PrimitiveSerializer implements ClaraSerializer {

        private final MimeType mimeType;

        PrimitiveSerializer(MimeType mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public ByteBuffer write(Object data) throws ClaraException {
            var bytes = switch (mimeType) {
                case INT32 -> Int32Value.of((Integer) data).toByteArray();
                case INT64 -> Int64Value.of((Long) data).toByteArray();
                case FLOAT -> FloatValue.of((Float) data).toByteArray();
                case DOUBLE -> DoubleValue.of((Double) data).toByteArray();
                default -> throw new IllegalStateException("invalid mime-type: " + mimeType);
            };
            return ByteBuffer.wrap(bytes);
        }

        @Override
        public Object read(ByteBuffer data) throws ClaraException {
            var bytes = data.array();
            try {
                return switch (mimeType) {
                    case INT32 -> Int32Value.parseFrom(bytes).getValue();
                    case INT64 -> Int64Value.parseFrom(bytes).getValue();
                    case FLOAT -> FloatValue.parseFrom(bytes).getValue();
                    case DOUBLE -> DoubleValue.parseFrom(bytes).getValue();
                    default -> throw new IllegalStateException("Invalid mime-type: " + mimeType);
                };
            } catch (InvalidProtocolBufferException e) {
                throw new ClaraException(e.getMessage());
            }
        }
    }
}
