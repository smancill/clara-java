/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.net.SocketFactory;
import org.jlab.clara.msg.sys.utils.LogUtils;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.util.Collections;
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

    private final RegDatabase publishers = new RegDatabase();
    private final RegDatabase subscribers = new RegDatabase();

    private final RegAddress regAddress;
    private final String sender;

    private final Socket regSocket;
    private final SocketFactory factory;

    private static final Set<RegData> NO_DATA = Collections.emptySet();

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
        regSocket = factory.createSocket(SocketType.REP);
        try {
            factory.bindSocket(regSocket, regAddress.port());
        } catch (Exception e) {
            factory.closeQuietly(regSocket);
            throw e;
        }

        sender = address + "registrar";
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
        String topic = RegConstants.UNDEFINED;
        RegRequest request;
        RegResponse reply;
        try {
            request = new RegRequest(requestMsg);
            topic = request.topic();

            reply = switch (topic) {
                case RegConstants.REGISTER_PUBLISHER -> {
                    var data = request.data();
                    logRegistration("registered", "publisher ", data);
                    publishers.register(data);
                    yield response(topic, NO_DATA);
                }
                case RegConstants.REGISTER_SUBSCRIBER -> {
                    var data = request.data();
                    logRegistration("registered", "subscriber", data);
                    subscribers.register(data);
                    yield response(topic, NO_DATA);
                }
                case RegConstants.REMOVE_PUBLISHER -> {
                    var data = request.data();
                    logRegistration("removed", "publisher ", data);
                    publishers.remove(data);
                    yield response(topic, NO_DATA);
                }
                case RegConstants.REMOVE_SUBSCRIBER -> {
                    var data = request.data();
                    logRegistration("removed", "subscriber", data);
                    subscribers.remove(data);
                    yield response(topic, NO_DATA);
                }
                case RegConstants.REMOVE_ALL_REGISTRATION -> {
                    var host = request.text();
                    LOGGER.fine(() -> "removed all host = " + host);
                    publishers.remove(host);
                    subscribers.remove(host);
                    yield response(topic, NO_DATA);
                }
                case RegConstants.FIND_PUBLISHER -> {
                    var data = request.data();
                    logDiscovery("publishers ", data);
                    var rs = publishers.find(data.getDomain(), data.getSubject(), data.getType());
                    yield response(topic, rs);
                }
                case RegConstants.FIND_SUBSCRIBER -> {
                    var data = request.data();
                    logDiscovery("subscribers", data);
                    var rs = subscribers.rfind(data.getDomain(), data.getSubject(), data.getType());
                    yield response(topic, rs);
                }
                case RegConstants.FILTER_PUBLISHER -> {
                    var data = request.data();
                    logFilter("publishers ", data);
                    yield response(topic, publishers.filter(data));
                }
                case RegConstants.FILTER_SUBSCRIBER -> {
                    var data = request.data();
                    logFilter("subscribers", data);
                    yield response(topic, subscribers.filter(data));
                }
                case RegConstants.EXACT_PUBLISHER -> {
                    var data = request.data();
                    logFilter("publishers with exact topic ", data);
                    var rs = publishers.same(data.getDomain(), data.getSubject(), data.getType());
                    yield response(topic, rs);
                }
                case RegConstants.EXACT_SUBSCRIBER -> {
                    var data = request.data();
                    logFilter("subscribers with exact topic ", data);
                    var rs = subscribers.same(data.getDomain(), data.getSubject(), data.getType());
                    yield response(topic, rs);
                }
                case RegConstants.ALL_PUBLISHER -> {
                    LOGGER.fine(() -> "get all publishers");
                    yield response(topic, publishers.all());
                }
                case RegConstants.ALL_SUBSCRIBER -> {
                    LOGGER.fine(() -> "get all subscribers");
                    yield response(topic, subscribers.all());
                }
                default -> {
                    LOGGER.warning("unknown registration request type: " + topic);
                    yield response(topic, "unknown registration request");
                }
            };
        } catch (ClaraMsgException | InvalidProtocolBufferException e) {
            LOGGER.warning(LogUtils.exceptionReporter(e));
            reply = response(topic, e.getMessage());
        }
        return reply.msg();
    }

    private RegResponse response(String topic, Set<RegData> data) {
        return new RegResponse(topic, sender, data);
    }

    private RegResponse response(String topic, String msg) {
        return new RegResponse(topic, sender, msg);
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
