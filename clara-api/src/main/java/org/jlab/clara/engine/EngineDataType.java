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
import java.util.Arrays;
import java.util.Objects;

/**
 * Defines a data type used by a {@link Engine service engine}.
 * Data type can be a predefined type, or a custom-defined type.
 * When declaring a custom type, its serialization routine must be provided.
 */
public class EngineDataType {

    /**
     * Signed int of 32 bits.
     *
     * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding">Wire types</a>
     */
    public static final EngineDataType SINT32 = buildPrimitive(MimeType.SINT32);
    /**
     * Signed int of 64 bits.
     *
     * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding">Wire types</a>
     */
    public static final EngineDataType SINT64 = buildPrimitive(MimeType.SINT64);
    /**
     * Signed fixed integer of 32 bits.
     *
     * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding">Wire types</a>
     */
    public static final EngineDataType SFIXED32 = buildPrimitive(MimeType.SFIXED32);
    /**
     * Signed fixed integer of 64 bits.
     *
     * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding">Wire types</a>
     */
    public static final EngineDataType SFIXED64 = buildPrimitive(MimeType.SFIXED64);
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
    public static final EngineDataType STRING = buildPrimitive(MimeType.STRING);
    /**
     * Raw bytes.
     * On Java a {@link ByteBuffer} is used to wrap the byte array and its endianness.
     */
    public static final EngineDataType BYTES = buildRawBytes();
    /**
     * An array of signed varints of 32 bits.
     *
     * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding">Wire types</a>
     */
    public static final EngineDataType ARRAY_SINT32 = buildPrimitive(MimeType.ARRAY_SINT32);
    /**
     * An array of signed varints of 64 bits.
     *
     * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding">Wire types</a>
     */
    public static final EngineDataType ARRAY_SINT64 = buildPrimitive(MimeType.ARRAY_SINT64);
    /**
     * An array of signed fixed integers of 32 bits.
     *
     * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding">Wire types</a>
     */
    public static final EngineDataType ARRAY_SFIXED32 = buildPrimitive(MimeType.ARRAY_SFIXED32);
    /**
     * An array of signed fixed integers of 64 bits.
     *
     * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding">Wire types</a>
     */
    public static final EngineDataType ARRAY_SFIXED64 = buildPrimitive(MimeType.ARRAY_SFIXED64);
    /**
     * An array of floats (32 bits floating-point numbers).
     */
    public static final EngineDataType ARRAY_FLOAT = buildPrimitive(MimeType.ARRAY_FLOAT);
    /**
     * An array of doubles (64 bits floating-point numbers).
     */
    public static final EngineDataType ARRAY_DOUBLE = buildPrimitive(MimeType.ARRAY_DOUBLE);
    /**
     * An array of strings.
     */
    public static final EngineDataType ARRAY_STRING = buildPrimitive(MimeType.ARRAY_STRING);
    /**
     * JSON text.
     */
    public static final EngineDataType JSON = buildJson();
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

    private static EngineDataType buildJson() {
        return new EngineDataType(MimeType.JSON.toString(), new StringSerializer());
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
        SINT32          ("binary/sint32"),
        SINT64          ("binary/sint64"),
        SFIXED32        ("binary/sfixed32"),
        SFIXED64        ("binary/sfixed64"),
        FLOAT           ("binary/float"),
        DOUBLE          ("binary/double"),
        STRING          ("text/string"),
        BYTES           ("binary/bytes"),

        ARRAY_SINT32    ("binary/array-sint32"),
        ARRAY_SINT64    ("binary/array-sint64"),
        ARRAY_SFIXED32  ("binary/array-sfixed32"),
        ARRAY_SFIXED64  ("binary/array-sfixed32"),
        ARRAY_FLOAT     ("binary/array-float"),
        ARRAY_DOUBLE    ("binary/array-double"),
        ARRAY_STRING    ("binary/array-string"),
        ARRAY_BYTES     ("binary/array-string"),

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
                case SINT32 -> proto.setVLSINT32((Integer) data);
                case SINT64 -> proto.setVLSINT64((Long) data);
                case SFIXED32 -> proto.setFLSINT32((Integer) data);
                case SFIXED64 -> proto.setFLSINT64((Long) data);
                case DOUBLE -> proto.setDOUBLE((Double) data);
                case FLOAT -> proto.setFLOAT((Float) data);
                case STRING -> proto.setSTRING((String) data);
                case BYTES -> proto.setBYTES((ByteString) data);

                case ARRAY_SINT32 -> proto.addAllVLSINT32A(Arrays.asList((Integer[]) data));
                case ARRAY_SINT64 -> proto.addAllVLSINT64A(Arrays.asList((Long[]) data));
                case ARRAY_SFIXED32 -> proto.addAllFLSINT32A(Arrays.asList((Integer[]) data));
                case ARRAY_SFIXED64 -> proto.addAllFLSINT64A(Arrays.asList((Long[]) data));
                case ARRAY_DOUBLE -> proto.addAllDOUBLEA(Arrays.asList((Double[]) data));
                case ARRAY_FLOAT -> proto.addAllFLOATA(Arrays.asList((Float[]) data));
                case ARRAY_STRING -> proto.addAllSTRINGA(Arrays.asList((String[]) data));

                default -> throw new IllegalStateException("Invalid mime-type: " + mimeType);
            }
            return nativeSerializer.write(proto.build());
        }

        @Override
        public Object read(ByteBuffer data) throws ClaraException {
            var proto = (PlainData) nativeSerializer.read(data);
            return switch (mimeType) {
                case SINT32 -> proto.getVLSINT32();
                case SINT64 -> proto.getVLSINT64();
                case SFIXED32 -> proto.getFLSINT32();
                case SFIXED64 -> proto.getFLSINT64();
                case DOUBLE -> proto.getDOUBLE();
                case FLOAT -> proto.getFLOAT();
                case STRING -> proto.getSTRING();
                case BYTES -> proto.getBYTES();

                case ARRAY_SINT32 -> proto.getVLSINT32AList().toArray(new Integer[0]);
                case ARRAY_SINT64 -> proto.getVLSINT64AList().toArray(new Long[0]);
                case ARRAY_SFIXED32 -> proto.getFLSINT32AList().toArray(new Integer[0]);
                case ARRAY_SFIXED64 -> proto.getFLSINT64AList().toArray(new Long[0]);
                case ARRAY_DOUBLE -> proto.getDOUBLEAList().toArray(new Double[0]);
                case ARRAY_FLOAT -> proto.getFLOATAList().toArray(new Float[0]);
                case ARRAY_STRING -> proto.getSTRINGAList().toArray(new String[0]);

                default -> throw new IllegalStateException("Invalid mime-type: " + mimeType);
            };
        }
    }
}
