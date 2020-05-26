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
        ProxyDriver connection = ProxyDriver.subscriber(address, factory);
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
        ProxyDriver connection = ProxyDriver.publisher(address, factory);
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
        RegDriver driver = new RegDriver(address, factory);
        try {
            driver.connect();
            return driver;
        } catch (ZMQException | ClaraMsgException e) {
            driver.close();
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
