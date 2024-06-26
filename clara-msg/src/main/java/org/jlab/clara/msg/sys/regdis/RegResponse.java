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

import java.util.HashSet;
import java.util.Set;

/**
 * A wrapper for a response to a registration or discovery request.
 * <p>
 * A response of the {@link RegService registration service} can be a
 * string indicating that the request was successful, a set of registration data
 * in case a discovery request was received, or an error description indicating
 * that something wrong happened with the request.
 */
public class RegResponse {

    private final String action;
    private final String sender;
    private final String status;
    private final Set<RegData> data;

    /**
     * Constructs a success response. No registration data is returned.
     * The response status is set to
     * {@link RegConstants#SUCCESS}.
     * This response is used to signal that a request was successful.
     *
     * @param action the registrar action being responded
     * @param sender the sender of the response
     */
    public RegResponse(String action, String sender) {
        this.action = action;
        this.sender = sender;
        this.status = RegConstants.SUCCESS;
        this.data = new HashSet<>();
    }


    /**
     * Constructs a data response. The data can be an empty set.
     * The response status is set to
     * {@link RegConstants#SUCCESS}.
     * This response is used to return registration data for discovery requests.
     *
     * @param action the registrar action being responded
     * @param sender the sender of the response
     * @param data the registration data
     */
    public RegResponse(String action, String sender, Set<RegData> data) {
        this.action = action;
        this.sender = sender;
        this.status = RegConstants.SUCCESS;
        this.data = data;
    }


    /**
     * Passes String data response. The actual registration data is null.
     * The response status is an actual string representation of the data.
     * In case there is an error response status will indicate the description
     * of an error.
     *
     * @param action the registrar action being responded
     * @param sender the sender of the response
     * @param statusOrData the error description or string data.
     */
    public RegResponse(String action, String sender, String statusOrData) {
        this.action = action;
        this.sender = sender;
        this.status = statusOrData;
        this.data = new HashSet<>();
    }


    /**
     * De-serializes the response from the given message.
     *
     * @param msg the message with the response
     * @throws ClaraMsgException
     *         when the message is malformed or the data is corrupted,
     *         or when the response is an error
     *         (and the exception message is set to the error description)
     */
    public RegResponse(ZMsg msg) throws ClaraMsgException {

        if (msg.size() < 3) {
            throw new ClaraMsgException("invalid registrar server response format");
        }

        var actionFrame = msg.pop();
        var senderFrame = msg.pop();
        var statusFrame = msg.pop();

        action = new String(actionFrame.getData());
        sender = new String(senderFrame.getData());
        status = new String(statusFrame.getData());
        if (!status.equals(RegConstants.SUCCESS)) {
            throw new ClaraMsgException("registrar server could not process request: " + status);
        }

        data = new HashSet<>();
        while (!msg.isEmpty()) {
            var dataFrame = msg.pop();
            try {
                data.add(RegData.parseFrom(dataFrame.getData()));
            } catch (InvalidProtocolBufferException e) {
                throw new ClaraMsgException("could not parse registrar server response", e);
            }
        }
    }


    /**
     * Serializes the response into a message.
     *
     * @return a message containing the response
     */
    public ZMsg msg() {
        var msg = new ZMsg();
        msg.addString(action);
        msg.addString(sender);
        msg.addString(status);
        for (var reg : data) {
            msg.add(reg.toByteArray());
        }
        return msg;
    }


    /**
     * Returns the registrar action that created the response.
     */
    public String action() {
        return action;
    }


    /**
     * Returns the sender of the response.
     */
    public String sender() {
        return sender;
    }


    /**
     * Returns the status of the response.
     * It can be {@link RegConstants#SUCCESS}
     * or an error string indicating a problem with the request.
     */
    public String status() {
        return status;
    }


    /**
     * Returns the data of the response.
     * When the response is for indicating a success or error, the data is
     * empty. It can also be empty when no registration data is found for the
     * given request.
     */
    public Set<RegData> data() {
        return data;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + data.hashCode();
        result = prime * result + sender.hashCode();
        result = prime * result + status.hashCode();
        result = prime * result + action.hashCode();
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
        RegResponse other = (RegResponse) obj;
        if (!data.equals(other.data)) {
            return false;
        }
        if (!sender.equals(other.sender)) {
            return false;
        }
        if (!status.equals(other.status)) {
            return false;
        }
        return action.equals(other.action);
    }
}
