/*
 * Copyright (C) 2016. Jefferson Lab (JLAB). All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for governmental use, educational, research, and not-for-profit
 * purposes, without fee and without a signed licensing agreement.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL,
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY, PROVIDED
 * HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * This software was developed under the United States Government License.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
 */

package org.jlab.clara.msg.sys.regdis;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.net.SocketFactory;
import org.jlab.clara.msg.sys.utils.LogUtils;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.jlab.clara.msg.core.Topic.ANY;

/**
 * The main registrar service or registrar (names are used interchangeably).
 * Note that the object of this class always running in a separate thread.
 * This class is used by the Registrar executable.
 * <p>
 * Creates and maintains two separate databases to store publishers and subscribers
 * <p>
 * The following requests will be serviced:
 * <ul>
 *   <li>Register publisher</li>
 *   <li>Register subscriber</li>
 *   <li>Find publisher</li>
 *   <li>Find subscriber</li>
 * </ul>
 */
public class RegService implements Runnable {

    // Database to store publishers
    private final RegDatabase publishers = new RegDatabase();

    // database to store subscribers
    private final RegDatabase subscribers = new RegDatabase();

    // Address of the registrar
    private final RegAddress regAddress;

    // Address of the registrar
    private final Socket regSocket;

    private final SocketFactory factory;

    private static final Logger LOGGER = Logger.getLogger("Registrar");


    /**
     * Creates a registrar object.
     *
     * @param context the context to run the registrar service
     * @param address the address of the registrar service
     * @throws ClaraMsgException if the address is already in use
     */
    public RegService(Context context, RegAddress address) throws ClaraMsgException {
        factory = new SocketFactory(context.getContext());
        regAddress = address;
        regSocket = factory.createSocket(ZMQ.REP);
        try {
            factory.bindSocket(regSocket, regAddress.port());
        } catch (Exception e) {
            factory.closeQuietly(regSocket);
            throw e;
        }
    }

    /**
     * Returns the address of the registrar.
     */
    public RegAddress address() {
        return regAddress;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("running on host = " + regAddress.host() + "  port = " + regAddress.port());
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ZMsg request = ZMsg.recvMsg(regSocket);
                    if (request == null) {
                        break;
                    }
                    ZMsg reply = processRequest(request);
                    reply.send(regSocket);
                } catch (ZMQException e) {
                    if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                        break;
                    }
                    LOGGER.warning(LogUtils.exceptionReporter(e));
                }
            }
        } catch (Exception e) {
            LOGGER.severe(LogUtils.exceptionReporter(e));
        } finally {
            factory.closeQuietly(regSocket);
        }
    }

    /**
     * Registration request processing routine that runs in this thread.
     *
     * @param requestMsg serialized 0MQ message of the wire
     * @return serialized response: 0MQ message ready to go over the wire
     */
    ZMsg processRequest(ZMsg requestMsg) {

        // Preparing fields to furnish the response back.
        // Note these fields do not play any critical role what so ever, due to
        // the fact that registration is done using client-server type communication,
        // and are always synchronous.
        String topic = RegConstants.UNDEFINED;
        String sender = regAddress.host() + ":registrar";

        // response message
        RegResponse reply;

        try {
            // prepare the set to store registration info going back to the requester
            Set<RegData> registration = new HashSet<>();

            // create a RegRequest object from the serialized 0MQ message
            RegRequest request = new RegRequest(requestMsg);

            // retrieve the topic
            topic = request.topic();

            if (topic.equals(RegConstants.REGISTER_PUBLISHER)) {
                logRegistration("registered", "publisher ", request.data());
                publishers.register(request.data());

            } else if (topic.equals(RegConstants.REGISTER_SUBSCRIBER)) {
                logRegistration("registered", "subscriber", request.data());
                subscribers.register(request.data());

            } else if (topic.equals(RegConstants.REMOVE_PUBLISHER)) {
                logRegistration("removed", "publisher ", request.data());
                publishers.remove(request.data());

            } else if (topic.equals(RegConstants.REMOVE_SUBSCRIBER)) {
                logRegistration("removed", "subscriber", request.data());
                subscribers.remove(request.data());

            } else if (topic.equals(RegConstants.REMOVE_ALL_REGISTRATION)) {
                LOGGER.fine(() -> "removed all host = " + request.text());
                publishers.remove(request.text());
                subscribers.remove(request.text());

            } else if (topic.equals(RegConstants.FIND_PUBLISHER)) {
                RegData data = request.data();
                logDiscovery("publishers ", data);
                registration = publishers.find(data.getDomain(),
                                               data.getSubject(),
                                               data.getType());

            } else if (topic.equals(RegConstants.FIND_SUBSCRIBER)) {
                RegData data = request.data();
                logDiscovery("subscribers", data);
                registration = subscribers.rfind(data.getDomain(),
                                                 data.getSubject(),
                                                 data.getType());

            } else if (topic.equals(RegConstants.FILTER_PUBLISHER)) {
                RegData data = request.data();
                logFilter("publishers ", data);
                registration = publishers.filter(data);

            } else if (topic.equals(RegConstants.FILTER_SUBSCRIBER)) {
                RegData data = request.data();
                logFilter("subscribers", data);
                registration = subscribers.filter(data);

            } else if (topic.equals(RegConstants.EXACT_PUBLISHER)) {
                RegData data = request.data();
                logFilter("publishers with exact topic ", data);
                registration = publishers.same(data.getDomain(),
                                               data.getSubject(),
                                               data.getType());

            } else if (topic.equals(RegConstants.EXACT_SUBSCRIBER)) {
                RegData data = request.data();
                logFilter("subscribers with exact topic ", data);
                registration = subscribers.same(data.getDomain(),
                                                data.getSubject(),
                                                data.getType());

            } else if (topic.equals(RegConstants.ALL_PUBLISHER)) {
                LOGGER.fine(() -> "get all publishers");
                registration = publishers.all();

            } else if (topic.equals(RegConstants.ALL_SUBSCRIBER)) {
                LOGGER.fine(() -> "get all subscribers");
                registration = subscribers.all();

            }  else {
                LOGGER.warning("unknown registration request type: " + topic);
                reply = new RegResponse(topic, sender, "unknown registration request");
                return reply.msg();
            }

            reply = new RegResponse(topic, sender, registration);

        } catch (ClaraMsgException | InvalidProtocolBufferException e) {
            LOGGER.warning(LogUtils.exceptionReporter(e));
            reply = new RegResponse(topic, sender, e.getMessage());
        }

        return reply.msg();
    }


    private void logRegistration(String action, String type, RegData data) {
        LOGGER.fine(() -> String.format("%s %s name = %s  host = %s  port = %d  topic = %s:%s:%s",
                action, type, data.getName(),
                data.getHost(), data.getPort(),
                data.getDomain(), data.getSubject(), data.getType()));
    }

    private void logDiscovery(String type, RegData data) {
        LOGGER.fine(() -> String.format("search %s topic = %s:%s:%s",
                type, data.getDomain(), data.getSubject(), data.getType()));
    }

    private void logFilter(String type, RegData data) {
        LOGGER.fine(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("search ").append(type);
            if (!data.getDomain().equals(ANY)) {
                sb.append("  domain = ").append(data.getDomain());
            }
            if (!data.getSubject().equals(ANY)) {
                sb.append("  subject = ").append(data.getSubject());
            }
            if (!data.getType().equals(ANY)) {
                sb.append("  type = ").append(data.getType());
            }
            if (!data.getHost().equals(RegConstants.UNDEFINED)) {
                sb.append("  address = ")
                  .append(data.getHost()).append(':').append(data.getPort());
            }
            return sb.toString();
        });
    }
}
