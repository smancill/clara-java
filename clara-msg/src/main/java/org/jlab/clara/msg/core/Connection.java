/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.sys.pubsub.ProxyDriver;
import org.zeromq.ZMQException;

import java.io.Closeable;

/**
 * The standard connection to proxies.
 * <p>
 * Connections are managed by the actor, that keeps an internal connection
 * pool to cache and reuse connections.
 * The actor must be destroyed in order to close all connections cached in the
 * connection pool.
 * An external pool can also be used to shared connections between many actors.
 * <p>
 * Connections should be closed in order to return them to the connection pool,
 * so they can be reused by other publishing threads.
 */
public class Connection implements Closeable {

    private final ConnectionManager pool;
    private ProxyDriver connection;

    Connection(ConnectionManager pool, ProxyDriver connection) {
        this.pool = pool;
        this.connection = connection;
    }

    /**
     * If not destroyed, returns this connection the connection pool to be
     * reused.
     * Use {@link Actor#destroyConnection} to destroy the connection and actually
     * close the socket.
     */
    @Override
    public void close() {
        if (connection != null) {
            pool.releaseProxyConnection(connection);
            connection = null;
        }
    }

    void destroy() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    void publish(Message msg) throws ClaraMsgException {
        if (connection == null) {
            throw new IllegalStateException("connection is closed");
        }
        try {
            connection.send(msg.serialize());
        } catch (ZMQException e) {
            destroy();
            throw new ClaraMsgException("could not publish message", e);
        }
    }

    /**
     * Returns the address of the connected proxy.
     *
     * @return the address of the proxy
     */
    public ProxyAddress getAddress() {
        if (connection == null) {
            throw new IllegalStateException("connection is closed");
        }
        return connection.getAddress();
    }
}
