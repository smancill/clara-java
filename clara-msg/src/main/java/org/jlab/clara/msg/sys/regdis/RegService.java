/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jlab.clara.msg.core.Topic;
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
                    ZMsg response = processRequest(request);
                    response.send(regSocket);
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
        String action = RegConstants.UNDEFINED;
        RegRequest request;
        RegResponse reply;
        try {
            request = new RegRequest(requestMsg);
            action = request.action();

            var data = request.data();
            var database = switch (data.getOwnerType()) {
                case PUBLISHER -> publishers;
                case SUBSCRIBER -> subscribers;
            };

            reply = switch (action) {
                case RegConstants.REGISTER -> {
                    logRegistration("register", data);
                    database.register(data);
                    yield response(action, NO_DATA);
                }
                case RegConstants.REMOVE -> {
                    logRegistration("remove", data);
                    database.remove(data);
                    yield response(action, NO_DATA);
                }
                case RegConstants.REMOVE_ALL -> {
                    var host = data.getHost();
                    LOGGER.fine(() -> "remove all " + getType(data, true) + " from host = " + host);
                    database.remove(host);
                    yield response(action, NO_DATA);
                }
                case RegConstants.FIND_MATCHING -> {
                    var topic = getTopic(data);
                    var match = switch (data.getOwnerType()) {
                        case PUBLISHER -> RegDatabase.TopicMatch.PREFIX_MATCHING;
                        case SUBSCRIBER -> RegDatabase.TopicMatch.REVERSE_MATCHING;
                    };
                    logDiscovery(data, "matching");
                    yield response(action, database.find(topic, match));
                }
                case RegConstants.FILTER -> {
                    logFilter(data);
                    yield response(action, database.filter(data));
                }
                case RegConstants.FIND_EXACT -> {
                    var topic = getTopic(data);
                    var match = RegDatabase.TopicMatch.EXACT;
                    logDiscovery(data, "with");
                    yield response(action, database.find(topic, match));
                }
                case RegConstants.FIND_ALL -> {
                    LOGGER.fine(() -> "get all " + getType(data, true));
                    yield response(action, database.all());
                }
                default -> {
                    LOGGER.warning("unknown registration request: " + action);
                    yield response(action, "unknown registration request");
                }
            };
        } catch (ClaraMsgException | InvalidProtocolBufferException e) {
            LOGGER.warning(LogUtils.exceptionReporter(e));
            reply = response(action, e.getMessage());
        }
        return reply.msg();
    }

    private RegResponse response(String action, Set<RegData> data) {
        return new RegResponse(action, sender, data);
    }

    private RegResponse response(String action, String msg) {
        return new RegResponse(action, sender, msg);
    }

    private static void logRegistration(String action, RegData data) {
        LOGGER.fine(() -> String.format("%s %s name = %s  host = %s  port = %d  topic = %s",
                action, getType(data, false), data.getName(),
                data.getHost(), data.getPort(), getTopic(data)));
    }

    private static void logDiscovery(RegData data, String match) {
        LOGGER.fine(() -> String.format("search %s %s topic = %s",
                getType(data, true), match, getTopic(data)));
    }

    private static void logFilter(RegData data) {
        LOGGER.fine(() -> {
            var sb = new StringBuilder();
            sb.append("filter ").append(getType(data, true));
            if (!data.getDomain().equals(ANY)) {
                sb.append("  prefix = ").append(getTopic(data));
            }
            if (!data.getHost().equals(RegConstants.UNDEFINED)) {
                sb.append("  address = ").append(data.getHost());
                if (data.getPort() != 0) {
                    sb.append(':').append(data.getPort());
                }
            }
            return sb.toString();
        });
    }

    private static String getType(RegData data, boolean plural) {
        var suffix = plural ? "s" : "";
        return switch (data.getOwnerType()) {
            case PUBLISHER -> "publisher" + suffix + " ";
            case SUBSCRIBER -> "subscriber" + suffix;
        };
    }

    private static Topic getTopic(RegData data) {
        return Topic.build(data.getDomain(), data.getSubject(), data.getType());
    }
}
