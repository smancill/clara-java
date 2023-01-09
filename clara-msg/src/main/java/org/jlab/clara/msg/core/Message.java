/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jlab.clara.msg.data.MetaDataProto.MetaData;
import org.jlab.clara.msg.data.MimeType;
import org.jlab.clara.msg.data.PlainDataProto.PlainData;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.zeromq.ZMsg;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * The user-data message for pub/sub communications.
 * <p>
 * A message is composed of a <i>topic</i>, <i>metadata</i> and binary data.
 * The {@link Topic topic} to which the message is published will be used by
 * subscribers to filter messages of interest.
 * The {@link MetaData metadata} is a protobuf class with fields that can be
 * used to describe the data of the message and the communication request the
 * message is part of. At minimum, the {@code dataType} field is required to
 * indicate the mime-type of the binary data.
 * The data byte array contains the binary representation of the actual data of
 * the message. Helpers are provided to serialize primitive data types.
 * Complex objects must be serialized before creating the message
 * (i.e. applications must take care of the binary data format).
 * <p>
 * When a message is sync-published, the <i>metadata</i> will contain an
 * auto-generated {@code replyTopic} where the response can be published to.
 */
public class Message {

    private final Topic topic;
    private final MetaData.Builder metaData;
    private final byte[] data;

    /**
     * Constructs a new message.
     * The message will be published to the given topic.
     * The metadata must contain the mime-type describing the data.
     * <p>
     * The byte array will be owned by the message, thus, it cannot be modified
     * by the calling code after creating this message.
     * If the byte array must be reused or modified, pass the array to the
     * constructor as {@code data.clone()} to ensure the message keeps a copy of
     * the data and not the original array.
     *
     * @param topic    the topic of the message
     * @param metaData the metadata of the message
     * @param data     serialized data
     */
    public Message(Topic topic, MetaData.Builder metaData, byte[] data) {
        this.topic = topic;
        this.metaData = metaData;
        this.data = data;
    }

    /**
     * Constructs a new message.
     * The message will be published to the given topic.
     * The metadata will only contain the specified {@code mime-type} describing
     * the data.
     * <p>
     * The byte array will be owned by the message, thus, it cannot be modified
     * by the calling code after creating this message.
     * If the byte array must be reused or modified, pass the array to the
     * constructor as {@code data.clone()} to ensure the message keeps a copy of
     * the data and not the original array.
     *
     * @param topic    the topic of the message
     * @param mimeType the mime-type string for the data
     * @param data     serialized data
     */
    public Message(Topic topic, String mimeType, byte[] data) {
        this.topic = topic;
        this.metaData = MetaData.newBuilder();
        this.metaData.setDataType(mimeType);
        this.data = data;
    }

    /**
     *  Creates a message from the 0MQ message received from the wire.
     *
     * @param msg the received 0MQ message
     */
    Message(ZMsg msg) throws ClaraMsgException {

        if (msg.size() != 3) {
            throw new ClaraMsgException("invalid pub/sub message format");
        }

        var topicFrame = msg.pop();
        var metaDataFrame = msg.pop();
        var dataFrame = msg.pop();

        try {
            this.topic = Topic.wrap(topicFrame.getData());
            this.metaData = MetaData.parseFrom(metaDataFrame.getData()).toBuilder();
            this.data = dataFrame.getData();
        } catch (InvalidProtocolBufferException e) {
            throw new ClaraMsgException("could not parse metadata", e);
        }
    }

    /**
     * Serializes this message into a 0MQ message,
     * ready to send it over the wire.
     *
     * @return the 0MQ message
     */
    ZMsg serialize() {
        var msg = new ZMsg();
        msg.add(topic.toString());
        msg.add(metaData.build().toByteArray());
        msg.add(data);
        return msg;
    }

    /**
     * Returns the topic of the message.
     *
     * @return the topic to which the message is published
     */
    public Topic getTopic() {
        return topic;
    }

    /**
     * Returns the metadata of the message.
     *
     * @return a reference to the metadata of the message
     */
    public MetaData.Builder getMetaData() {
        return metaData;
    }

    /**
     * Returns the mime-type of the message data.
     *
     * @return a string with the mime-type
     */
    public String getMimeType() {
        return metaData.getDataType();
    }

    /**
     * Checks if the message has a reply topic.
     * If true, the message is part of a sync-publish request and a response is
     * expected to be published to the reply topic.
     *
     * @return true if the message was sent as a sync-publish request, false otherwise
     */
    public boolean hasReplyTopic() {
        return metaData.hasReplyTo();
    }

    /**
     * Returns the topic this message should be replied to.
     *
     * @return the topic to publish the response for this message
     */
    public Topic getReplyTopic() {
        return Topic.wrap(metaData.getReplyTo());
    }

    /**
     * Checks if the metadata contains byte order information.
     *
     * @return true if the data must be used with a certain byte-order
     */
    public boolean hasDataOrder() {
        return metaData.hasByteOrder();
    }

    /**
     * Returns the byte order of the data, if set.
     * If the byte order is not set in the metadata,
     * the returned value is {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}.
     *
     * @return the byte-order on which the data must be accessed
     */
    public ByteOrder getDataOrder() {
        if (!metaData.hasByteOrder()) {
            return ByteOrder.BIG_ENDIAN;
        }
        return switch (metaData.getByteOrder()) {
            case Big -> ByteOrder.BIG_ENDIAN;
            case Little -> ByteOrder.LITTLE_ENDIAN;
        };
    }

    /**
     * Returns the size of the byte array containing the data.
     *
     * @return the size of the data, in bytes
     */
    public int getDataSize() {
        return data != null ? data.length : 0;
    }

    /**
     * Returns the data of the message.
     *
     * @return the byte array with the raw message data
     */
    public byte[] getData() {
        return data;
    }


    /**
     * Constructs a message with the given data.
     * <p>
     * This method will do it's best to figure out the type of the object,
     * updating accordingly the data mime-type.
     * It will also serialize the object and store it as a byte array.
     * <p>
     * In case of passing a Java object, this will fail if the object is not
     * serializable.
     *
     * @param topic the topic of the message
     * @param data the data of the message
     * @return a new message containing the given data
     * @throws UncheckedIOException if data is a Java object and serialization failed
     * @see MimeType
     */
    public static Message createFrom(Topic topic, Object data) {

        byte[] ba = null;
        final String mimeType;
        PlainData.Builder pd = PlainData.newBuilder();

        if (data instanceof String value) {
            mimeType = MimeType.STRING;
            ba = value.getBytes(StandardCharsets.UTF_8);

        } else if (data instanceof Integer value) {
            mimeType = MimeType.INT32;
            pd.setFLSINT32(value);

        } else if (data instanceof Long value) {
            mimeType = MimeType.INT64;
            pd.setFLSINT64(value);

        } else if (data instanceof Float value) {
            mimeType = MimeType.FLOAT;
            pd.setFLOAT(value);

        } else if (data instanceof Double value) {
            mimeType = MimeType.DOUBLE;
            pd.setDOUBLE(value);

        } else if (data instanceof byte[] bytes) {
            mimeType = MimeType.BYTES;
            ba = bytes;

        } else {
            mimeType = MimeType.JOBJECT;
            try {
                ba = ActorUtils.serializeToBytes(data);
            } catch (IOException e) {
                throw new UncheckedIOException("could not serialize object", e);
            }
        }

        if (ba == null) {
            ba = pd.build().toByteArray();
        }

        return new Message(topic, mimeType, ba);
    }


    /**
     * Deserializes data from the given message.
     *
     * @param message the message that contains the required data
     * @return the deserialized data of the message
     */
    public static Object parseData(Message message) {
        try {
            byte[] data = message.getData();
            String dataType = message.getMimeType();

            if (dataType.equals(MimeType.STRING)) {
                return new String(data, StandardCharsets.UTF_8);

            } else if (dataType.equals(MimeType.INT32)) {
                var pd = PlainData.parseFrom(data);
                if (pd.hasFLSINT32()) {
                    return pd.getFLSINT32();
                }

            } else if (dataType.equals(MimeType.INT64)) {
                var pd = PlainData.parseFrom(data);
                if (pd.hasFLSINT64()) {
                    return pd.getFLSINT64();
                }

            } else if (dataType.equals(MimeType.FLOAT)) {
                var pd = PlainData.parseFrom(data);
                if (pd.hasFLOAT()) {
                    return pd.getFLOAT();
                }

            } else if (dataType.equals(MimeType.DOUBLE)) {
                var pd = PlainData.parseFrom(data);
                if (pd.hasDOUBLE()) {
                    return pd.getDOUBLE();
                }

            } else {
                try {
                    return ActorUtils.deserialize(data);
                } catch (ClassNotFoundException | IOException e) {
                    throw new RuntimeException("could not deserialize data", e);
                }
            }

            throw new IllegalArgumentException("the message data doesn't match the mime-type:"
                                               + dataType);

        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("message doesn't contain a valid data buffer");
        }
    }


    /**
     * Deserializes simple data from the given message.
     * Useful when the message was created with {@link #createFrom}.
     * The message should contain data of the given {@code dataType}.
     *
     * @param <T> the expected type of the data
     * @param message the message that contains the required data
     * @param dataType the type of the required data
     * @return the deserialized data of the message,
     *         if the message contains data of the given type
     * @throws IllegalArgumentException if the message does not contain data
     *         of the expected type
     * @see MimeType
     */
    public static <T> T parseData(Message message, Class<T> dataType) {
        try {
            byte[] data = message.getData();

            if (dataType.equals(String.class)) {
                var value = new String(data, StandardCharsets.UTF_8);
                return dataType.cast(value);

            } else if (dataType.equals(Integer.class)) {
                var pd = PlainData.parseFrom(data);
                if (pd.hasFLSINT32()) {
                    return dataType.cast(pd.getFLSINT32());
                }

            } else if (dataType.equals(Long.class)) {
                var pd = PlainData.parseFrom(data);
                if (pd.hasFLSINT64()) {
                    return dataType.cast(pd.getFLSINT64());
                }

            } else if (dataType.equals(Float.class)) {
                var pd = PlainData.parseFrom(data);
                if (pd.hasFLOAT()) {
                    return dataType.cast(pd.getFLOAT());
                }

            } else if (dataType.equals(Double.class)) {
                var pd = PlainData.parseFrom(data);
                if (pd.hasDOUBLE()) {
                    return dataType.cast(pd.getDOUBLE());
                }

            } else if (dataType.equals(Object.class)) {
                try {
                    return dataType.cast(ActorUtils.deserialize(data));
                } catch (ClassNotFoundException | IOException e) {
                    throw new RuntimeException("could not deserialize data", e);
                }
            }

            throw new IllegalArgumentException("message doesn't contain data of type: " + dataType);

        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("message doesn't contain a valid data buffer");
        }
    }


    /**
     * Creates a response to the given message, using the same data.
     * The message must contain the <i>replyTo</i> metadata field.
     *
     * @param msg the received message to be responded
     * @return a response message with the proper topic and the same received data
     */
    public static Message createResponse(Message msg) {
        var topic = Topic.wrap(msg.metaData.getReplyTo());
        var meta = MetaData.newBuilder(msg.metaData.build());
        meta.clearReplyTo();
        return new Message(topic, meta, msg.data);
    }

    /**
     * Creates a response to the given message, serializing the given data.
     * The message must contain the <i>replyTo</i> metadata field.
     *
     * @param msg the received message to be responded
     * @param data the data to be sent back
     * @return a response message with the proper topic and the given data
     */
    public static Message createResponse(Message msg, Object data) {
        var topic = Topic.wrap(msg.metaData.getReplyTo());
        return createFrom(topic, data);
    }
}
