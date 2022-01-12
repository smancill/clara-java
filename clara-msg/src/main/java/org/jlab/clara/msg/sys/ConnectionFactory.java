/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.net.SocketFactory;
import org.jlab.clara.msg.sys.pubsub.ProxyDriver;
import org.jlab.clara.msg.sys.pubsub.ProxyDriverSetup;
import org.jlab.clara.msg.sys.regdis.RegDriver;
import org.jlab.clara.msg.sys.utils.ThreadUtils;
import org.zeromq.ZMQException;

/**
 * Creates new connections.
 */
public class ConnectionFactory {

    private final Context context;
    private final SocketFactory factory;

    /**
     * Creates a new connection factory.
     *
     * @param context the context to handle connections
     */
    public ConnectionFactory(Context context) {
        this.context = context;
        this.factory = new SocketFactory(context.getContext());
    }

    /**
     * Creates a new subscriber connection.
     *
     * @param address the address of the proxy used by the connection
     * @param setup the settings of the connection
     * @return a new connection that can subscribe to topics
     *         through the proxy running on the given address
     * @throws ClaraMsgException if the connection could not be created
     */
    public ProxyDriver createSubscriberConnection(ProxyAddress address,
                                                  ProxyDriverSetup setup)
            throws ClaraMsgException {
        var connection = ProxyDriver.subscriber(address, factory);
        prepareProxyConnection(connection, setup);
        return connection;
    }

    /**
     * Creates a new publisher connection.
     *
     * @param address the address of the proxy used by the connection
     * @param setup the settings of the connection
     * @return a new connection that can publish to topics
     *         through the proxy running on the given address
     * @throws ClaraMsgException if the connection could not be created
     */
    public ProxyDriver createPublisherConnection(ProxyAddress address,
                                                 ProxyDriverSetup setup)
            throws ClaraMsgException {
        var connection = ProxyDriver.publisher(address, factory);
        prepareProxyConnection(connection, setup);
        return connection;
    }

    private void prepareProxyConnection(ProxyDriver connection, ProxyDriverSetup setup)
            throws ClaraMsgException {
        try {
            setup.preConnection(connection.getSocket());
            connection.connect();
            ThreadUtils.sleep(10);
            if (setup.checkConnection() && !connection.checkConnection(setup.connectionTimeout())) {
                throw new ClaraMsgException("could not connect to " + connection.getAddress());
            }
            setup.postConnection();
        } catch (ZMQException | ClaraMsgException e) {
            connection.close();
            throw e;
        }
    }

    /**
     * Creates a new registrar connection.
     *
     * @param address the address of the registrar used by the connection
     * @return a new connection that can communicate with
     *         the registrar running on the given address
     * @throws ClaraMsgException if the connection could not be created
     */
    public RegDriver createRegistrarConnection(RegAddress address) throws ClaraMsgException {
        var connection = new RegDriver(address, factory);
        try {
            connection.connect();
            return connection;
        } catch (ZMQException | ClaraMsgException e) {
            connection.close();
            throw e;
        }
    }

    /**
     * Returns the context used by this factory.
     *
     * @return the context
     */
    public Context getContext() {
        return context;
    }
}
