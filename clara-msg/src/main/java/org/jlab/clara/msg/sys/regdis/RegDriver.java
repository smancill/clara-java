/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis;

import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.net.SocketFactory;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.util.Set;

/**
 * Registration driver.
 *
 * Provides methods for registration and discovery of actors (i.e.
 * publishers and subscribers) on the specified
 * {@link RegService registrar service},
 * using a 0MQ REQ socket.
 */
public class RegDriver {

    private final RegAddress address;
    private final SocketFactory factory;
    private final Socket socket;

    /**
     * Creates a driver to the registrar running in the given address.
     *
     * @param address registrar service address
     * @param factory factory for the ZMQ socket
     * @throws ClaraMsgException
     */
    public RegDriver(RegAddress address, SocketFactory factory) throws ClaraMsgException {
        this.address = address;
        this.factory = factory;
        this.socket = factory.createSocket(SocketType.REQ);
    }

    /**
     * Connects to the registrar server.
     *
     * @throws ClaraMsgException if the connection failed
     */
    public void connect() throws ClaraMsgException {
        factory.connectSocket(socket, address.host(), address.port());
    }

    /**
     * Sends a request to the registrar server and waits the response.
     *
     * @param request the registration request
     * @param timeout timeout in milliseconds
     *
     * @return the registrar response
     */
    protected RegResponse request(RegRequest request, long timeout)
            throws ClaraMsgException {
        var requestMsg = request.msg();
        try {
            requestMsg.send(socket);
        } catch (ZMQException e) {
            throw new ClaraMsgException("could not send registration request", e);
        }

        try (var poller = factory.context().poller(1)) {
            poller.register(socket, ZMQ.Poller.POLLIN);
            poller.poll(timeout);
            if (poller.pollin(0)) {
                var response = new RegResponse(ZMsg.recvMsg(socket));
                var status = response.status();
                if (!status.equals(RegConstants.SUCCESS)) {
                    throw new ClaraMsgException("registrar server could not process request: "
                                            + status);
                }
                return response;
            } else {
                throw new ClaraMsgException("registrar server response timeout");
            }
        }
    }

    /**
     * Sends a registration request to the registrar service.
     *
     * @param sender the sender of the request
     * @param data the registration data
     * @throws ClaraMsgException
     */
    public void addRegistration(String sender, RegData data)
            throws ClaraMsgException {
        addRegistration(sender, data, RegConstants.REGISTRATION_TIMEOUT);
    }

    /**
     * Sends a registration request to the registrar service.
     *
     * @param sender the sender of the request
     * @param data the registration data
     * @param timeout the milliseconds to wait for a response
     * @throws ClaraMsgException
     */
    public void addRegistration(String sender, RegData data, long timeout)
            throws ClaraMsgException {
        var topic = selectTopic(data.getOwnerType(),
                                RegConstants.REGISTER_PUBLISHER,
                                RegConstants.REGISTER_SUBSCRIBER);
        var request = new RegRequest(topic, sender, data);
        request(request, timeout);
    }

    /**
     * Sends a remove registration request to the registrar service.
     *
     * @param sender the sender of the request
     * @param data the registration data
     * @throws ClaraMsgException
     */
    public void removeRegistration(String sender, RegData data)
            throws ClaraMsgException {
        removeRegistration(sender, data, RegConstants.REGISTRATION_TIMEOUT);
    }

    /**
     * Sends a remove registration request to the registrar service.
     *
     * @param sender the sender of the request
     * @param data the registration data
     * @param timeout the milliseconds to wait for a response
     * @throws ClaraMsgException
     */
    public void removeRegistration(String sender, RegData data, long timeout)
            throws ClaraMsgException {
        var topic = selectTopic(data.getOwnerType(),
                                RegConstants.REMOVE_PUBLISHER,
                                RegConstants.REMOVE_SUBSCRIBER);
        var request = new RegRequest(topic, sender, data);
        request(request, timeout);
    }

    /**
     * Removes registration of all actors of the specified node.
     * This will remove all publishers and subscribers running
     * on the given host from the registrar service connected
     * by this driver.
     *
     * @param sender the sender of the request
     * @param host the host of the actors to be removed
     * @throws ClaraMsgException
     */
    public void removeAllRegistration(String sender, String host)
            throws ClaraMsgException {
        removeAllRegistration(sender, host, RegConstants.REGISTRATION_TIMEOUT);
    }

    /**
     * Removes registration of all actors of the specified node.
     * This will remove all publishers and subscribers running
     * on the given host from the registrar service connected
     * by this driver.
     *
     * @param sender the sender of the request
     * @param host the host of the actors to be removed
     * @param timeout the milliseconds to wait for a response
     * @throws ClaraMsgException
     */
    public void removeAllRegistration(String sender, String host, long timeout)
            throws ClaraMsgException {
        var topic = RegConstants.REMOVE_ALL_REGISTRATION;
        var request = new RegRequest(topic, sender, host);
        request(request, timeout);
    }

    /**
     * Sends a request to search the database for publishers or subscribers
     * to a specific topic to the registrar server and waits the response.
     * The topic of interest is defined within the given registration data.
     *
     * @param sender the sender of the request
     * @param data the registration data object
     * @return set of publishers or subscribers to the required topic.
     * @throws ClaraMsgException
     */
    public Set<RegData> findRegistration(String sender, RegData data)
            throws ClaraMsgException {
        return findRegistration(sender, data, RegConstants.DISCOVERY_TIMEOUT);
    }

    /**
     * Sends a request to search the database for publishers or subscribers
     * to a specific topic to the registrar server and waits the response.
     * The topic of interest is defined within the given registration data.
     *
     * @param sender the sender of the request
     * @param data the registration data object
     * @param timeout the milliseconds to wait for a response
     * @return set of publishers or subscribers to the required topic.
     * @throws ClaraMsgException
     */
    public Set<RegData> findRegistration(String sender, RegData data, long timeout)
            throws ClaraMsgException {
        var topic = selectTopic(data.getOwnerType(),
                                RegConstants.FIND_PUBLISHER,
                                RegConstants.FIND_SUBSCRIBER);
        var request = new RegRequest(topic, sender, data);
        var response = request(request, timeout);
        return response.data();
    }

    /**
     * Sends a request to the registrar server to search the database
     * for publishers or subscribers matching specific terms, and waits the response.
     * The search terms should be set in the given registration data. They can be:
     * <ul>
     * <li>domain
     * <li>subject
     * <li>type
     * <li>address
     * </ul>
     * Only defined terms will be used for matching actors.
     * To topic parts should be undefined with {@link org.jlab.clara.msg.core.Topic#ANY}.
     * The address should be undefined with {@link RegConstants#UNDEFINED}.
     * The name is ignored.
     *
     * @param sender the sender of the request
     * @param data the registration data object
     * @return set of publishers or subscribers that match the given terms.
     * @throws ClaraMsgException
     */
    public Set<RegData> filterRegistration(String sender, RegData data)
            throws ClaraMsgException {
        return filterRegistration(sender, data, RegConstants.DISCOVERY_TIMEOUT);
    }

    /**
     * Sends a request to the registrar server to search the database
     * for publishers or subscribers matching specific terms, and waits the response.
     * The search terms should be set in the given registration data. They can be:
     * <ul>
     * <li>domain
     * <li>subject
     * <li>type
     * <li>address
     * </ul>
     * Only defined terms will be used for matching actors.
     * To topic parts should be undefined with {@link org.jlab.clara.msg.core.Topic#ANY}.
     * The address should be undefined with {@link RegConstants#UNDEFINED}.
     * The name is ignored.
     *
     * @param sender the sender of the request
     * @param data the registration data object
     * @param timeout the milliseconds to wait for a response
     * @return set of publishers or subscribers that match the given terms.
     * @throws ClaraMsgException
     */
    public Set<RegData> filterRegistration(String sender, RegData data, long timeout)
            throws ClaraMsgException {
        var topic = selectTopic(data.getOwnerType(),
                                RegConstants.FILTER_PUBLISHER,
                                RegConstants.FILTER_SUBSCRIBER);
        var request = new RegRequest(topic, sender, data);
        var response = request(request, timeout);
        return response.data();
    }

    /**
     * Sends a request to the registrar server to search the database
     * for publishers or subscribers with exactly the same specific topic,
     * and waits the response.
     * <p>
     * This method finds actors with the same topic, instead of doing prefix
     * matching like {@link #findRegistration} or comparing individual topic
     * parts like {@link #filterRegistration}. If the requested topic is {@code A:B},
     * this method will only return actors with topic {@code A:B}, while the
     * filter and find methods will also return actors with matching topics,
     * like {@code A:B:C}.
     * <p>
     * The topic of interest is defined within the given registration data.
     *
     * @param sender the sender of the request
     * @param data the registration data object
     * @return set of publishers or subscribers that have the exact same topic.
     * @throws ClaraMsgException
     */
    public Set<RegData> sameRegistration(String sender, RegData data)
            throws ClaraMsgException {
        return sameRegistration(sender, data, RegConstants.DISCOVERY_TIMEOUT);
    }

    /**
     * Sends a request to the registrar server to search the database
     * for publishers or subscribers with exactly the same specific topic,
     * and waits the response.
     * <p>
     * This method finds actors with the same topic, instead of doing prefix
     * matching like {@link #findRegistration} or comparing individual topic
     * parts like {@link #filterRegistration}. If the requested topic is {@code A:B},
     * this method will only return actors with topic {@code A:B}, while the
     * filter and find methods will also return actors with matching topics,
     * like {@code A:B:C}.
     * <p>
     * The topic of interest is defined within the given registration data.
     *
     * @param sender the sender of the request
     * @param data the registration data object
     * @param timeout the milliseconds to wait for a response
     * @return set of publishers or subscribers that have the exact same topic.
     * @throws ClaraMsgException
     */
    public Set<RegData> sameRegistration(String sender, RegData data, long timeout)
            throws ClaraMsgException {
        var topic = selectTopic(data.getOwnerType(),
                                RegConstants.EXACT_PUBLISHER,
                                RegConstants.EXACT_SUBSCRIBER);
        var request = new RegRequest(topic, sender, data);
        var response = request(request, timeout);
        return response.data();
    }

    /**
     * Sends a request to the database to get all publishers or subscribers,
     * and waits the response.
     *
     * @param sender the sender of the request
     * @param data the registration data object
     * @return set of publishers or subscribers to the required topic.
     * @throws ClaraMsgException
     */
    public Set<RegData> allRegistration(String sender, RegData data)
            throws ClaraMsgException {
        return allRegistration(sender, data, RegConstants.DISCOVERY_TIMEOUT);
    }

    /**
     * Sends a request to the database to get all publishers or subscribers,
     * and waits the response.
     *
     * @param sender the sender of the request
     * @param data the registration data object
     * @param timeout the milliseconds to wait for a response
     * @return set of publishers or subscribers to the required topic.
     * @throws ClaraMsgException
     */
    public Set<RegData> allRegistration(String sender, RegData data, long timeout)
            throws ClaraMsgException {
        var topic = selectTopic(data.getOwnerType(),
                                RegConstants.ALL_PUBLISHER,
                                RegConstants.ALL_SUBSCRIBER);
        var request = new RegRequest(topic, sender, data);
        var response = request(request, timeout);
        return response.data();
    }


    /**
     * Closes the connection to the registrar.
     */
    public void close() {
        factory.closeQuietly(socket);
    }

    /**
     * Returns the address of the registrar service.
     */
    public RegAddress getAddress() {
        return address;
    }


    private static String selectTopic(RegData.OwnerType type, String pubTopic, String subTopic) {
        return switch (type) {
            case PUBLISHER -> pubTopic;
            case SUBSCRIBER -> subTopic;
        };
    }
}
