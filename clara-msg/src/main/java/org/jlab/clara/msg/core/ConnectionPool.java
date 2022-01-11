/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.sys.ConnectionFactory;
import org.jlab.clara.msg.sys.pubsub.ProxyDriver;

import java.io.Closeable;

/**
 * A connection pool that can be shared between actors.
 */
public final class ConnectionPool implements Closeable {

    final ConnectionSetup setup;
    final ConnectionManager connectionManager;

    /**
     * Creates a builder to create a new connection pool.
     *
     * @return a new ConnectionPool builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }


    /**
     * Helps creating and configuring a connection pool.
     * All parameters not set will be initialized to their default values.
     */
    public static final class Builder extends ConnectionSetup.Builder<Builder> {

        @Override
        Builder getThis() {
            return this;
        }

        /**
         * Creates the connection pool with the defined setup.
         *
         * @return a new connection pool
         */
        public ConnectionPool build() {
            ConnectionSetup setup = new ConnectionSetup(proxyAddress, conSetup.build());
            ConnectionFactory factory = new ConnectionFactory(Context.getInstance());
            return new ConnectionPool(setup, factory);
        }
    }


    /**
     * Default constructor.
     */
    private ConnectionPool(ConnectionSetup setup,
                           ConnectionFactory factory) {
        this.setup = setup;
        this.connectionManager = new ConnectionManager(factory, setup.connectionSetup());
    }

    /**
     * Closes all connections.
     */
    public void destroy() {
        final int infiniteLinger = -1;
        destroy(infiniteLinger);
    }

    /**
     * Closes all connections.
     *
     * @param linger the linger period when closing the sockets
     * @see <a href="http://api.zeromq.org/3-2:zmq-setsockopt">ZMQ_LINGER</a>
     */
    public void destroy(int linger) {
        connectionManager.destroy(linger);
    }

    /**
     * Closes all connections.
     */
    @Override
    public void close() {
        destroy();
    }

    /**
     * Obtains a connection to the default proxy.
     * If there is no available connection, a new one will be created.
     * <p>
     * Creating new connections takes some time, and the first published
     * messages may be lost. The {@link #cacheConnection()} method can be used
     * to create connections before using them .
     *
     * @return a connection to the proxy
     * @throws ClaraMsgException if a new connection could not be created
     */
    public Connection getConnection() throws ClaraMsgException {
        return getConnection(setup.proxyAddress());
    }

    /**
     * Obtains a connection to the specified proxy.
     * If there is no available connection, a new one will be created.
     * <p>
     * Creating new connections takes some time, and the first published
     * messages may be lost. The {@link #cacheConnection(ProxyAddress)}
     * method can be used to create connections before using them .
     *
     * @param address the address of the proxy
     * @return a connection to the proxy
     * @throws ClaraMsgException if a new connection could not be created
     */
    public Connection getConnection(ProxyAddress address) throws ClaraMsgException {
        return new Connection(connectionManager,
                                  connectionManager.getProxyConnection(address));
    }

    /**
     * Creates and stores a connection to the default proxy.
     * Useful to ensure that there is a connection ready when {@link
     * #getConnection()} is called.
     *
     * @throws ClaraMsgException if the new connection could not be created
     */
    public void cacheConnection() throws ClaraMsgException {
        cacheConnection(setup.proxyAddress());
    }

    /**
     * Creates and stores a connection to the specified proxy.
     * Useful to ensure that there is a connection ready when {@link
     * #getConnection(ProxyAddress)} is called.
     *
     * @param address the address of the proxy
     * @throws ClaraMsgException if the new connection could not be created
     */
    public void cacheConnection(ProxyAddress address) throws ClaraMsgException {
        ProxyDriver connection = connectionManager.createProxyConnection(address);
        connectionManager.releaseProxyConnection(connection);
    }

    /**
     * Destroys the given connection.
     *
     * @param connection the connection to be destroyed
     */
    public void destroyConnection(Connection connection) {
        connection.destroy();
    }
}
