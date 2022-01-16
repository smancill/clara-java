/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.zeromq.ZMsg;

/**
 * A wrapper for a registration request.
 */
class RegRequest {

    private final String topic;
    private final String sender;
    private final RegData data;

    /**
     * Constructs a data request.
     *
     * @param topic the request being responded
     * @param sender the sender of the response
     * @param data the registration data of the request
     */
    RegRequest(String topic, String sender, RegData data) {
        this.topic = topic;
        this.sender = sender;
        this.data = data;
    }

    /**
     * De-serializes the request from the given message.
     *
     * @param msg the message with the response
     * @throws ClaraMsgException when the message is malformed
     * @throws InvalidProtocolBufferException when the data is corrupted
     */
    RegRequest(ZMsg msg) throws ClaraMsgException, InvalidProtocolBufferException {

        if (msg.size() != 3) {
            throw new ClaraMsgException("invalid registrar server request format");
        }

        var topicFrame = msg.pop();
        var senderFrame = msg.pop();
        var dataFrame = msg.pop();

        topic = new String(topicFrame.getData());
        sender = new String(senderFrame.getData());
        data = RegData.parseFrom(dataFrame.getData());
    }

    /**
     * Serializes the request into a message.
     *
     * @return a message containing the request
     */
    public ZMsg msg() {
        var msg = new ZMsg();
        msg.addString(topic);
        msg.addString(sender);
        msg.add(data.toByteArray());
        return msg;
    }

    /**
     * Returns the topic of the request.
     */
    public String topic() {
        return topic;
    }

    /**
     * Returns the sender of the request.
     */
    public String sender() {
        return sender;
    }

    /**
     * Returns the data of the request.
     */
    public RegData data() {
        return data;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + data.hashCode();
        result = prime * result + sender.hashCode();
        result = prime * result + topic.hashCode();
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RegRequest other = (RegRequest) obj;
        if (!data.equals(other.data)) {
            return false;
        }
        if (!sender.equals(other.sender)) {
            return false;
        }
        return topic.equals(other.topic);
    }
}
