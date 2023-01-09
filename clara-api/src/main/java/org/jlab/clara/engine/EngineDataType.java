/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.engine;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.msg.data.PlainDataProto.PayloadData;
import org.jlab.clara.msg.data.PlainDataProto.PlainData;

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

    /**
     * A native data object.
     */
    public static final EngineDataType NATIVE_DATA = buildNative();

    /**
     * A native payload object.
     */
    public static final EngineDataType NATIVE_PAYLOAD = buildPayload();

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

    private static EngineDataType buildNative() {
        return new EngineDataType(MimeType.NATIVE_PLAIN.toString(), new NativeSerializer());
    }

    private static EngineDataType buildPayload() {
        return new EngineDataType(MimeType.NATIVE_PAYLOAD.toString(), new PayloadSerializer());
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

        JSON            ("application/json"),

        NATIVE_PLAIN    ("binary/clara-plain"),
        NATIVE_PAYLOAD  ("binary/clara-payload");

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


    private static class NativeSerializer implements ClaraSerializer {

        @Override
        public ByteBuffer write(Object data) throws ClaraException {
            var xData = (PlainData) data;
            return ByteBuffer.wrap(xData.toByteArray());
        }

        @Override
        public Object read(ByteBuffer data) throws ClaraException {
            try {
                return PlainData.parseFrom(data.array());
            } catch (InvalidProtocolBufferException e) {
                throw new ClaraException(e.getMessage());
            }
        }
    }


    private static class PayloadSerializer implements ClaraSerializer {

        @Override
        public ByteBuffer write(Object data) throws ClaraException {
            var payload = (PayloadData) data;
            return ByteBuffer.wrap(payload.toByteArray());
        }

        @Override
        public Object read(ByteBuffer data) throws ClaraException {
            try {
                return PayloadData.parseFrom(data.array());
            } catch (InvalidProtocolBufferException e) {
                throw new ClaraException(e.getMessage());
            }
        }
    }


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
        private final NativeSerializer nativeSerializer = new NativeSerializer();

        PrimitiveSerializer(MimeType mimeType) {
            this.mimeType = mimeType;
        }

        @Override
        public ByteBuffer write(Object data) throws ClaraException {
            var proto = PlainData.newBuilder();
            switch (mimeType) {
                case INT32 -> proto.setVLSINT32((Integer) data);
                case INT64 -> proto.setVLSINT64((Long) data);
                case DOUBLE -> proto.setDOUBLE((Double) data);
                case FLOAT -> proto.setFLOAT((Float) data);
                case BYTES -> proto.setBYTES((ByteString) data);
                default -> throw new IllegalStateException("Invalid mime-type: " + mimeType);
            }
            return nativeSerializer.write(proto.build());
        }

        @Override
        public Object read(ByteBuffer data) throws ClaraException {
            var proto = (PlainData) nativeSerializer.read(data);
            return switch (mimeType) {
                case INT32 -> proto.getVLSINT32();
                case INT64 -> proto.getVLSINT64();
                case DOUBLE -> proto.getDOUBLE();
                case FLOAT -> proto.getFLOAT();
                case BYTES -> proto.getBYTES();
                default -> throw new IllegalStateException("Invalid mime-type: " + mimeType);
            };
        }
    }
}
