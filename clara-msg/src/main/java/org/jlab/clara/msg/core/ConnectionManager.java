/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.sys.ConnectionFactory;
import org.jlab.clara.msg.sys.pubsub.ProxyDriver;
import org.jlab.clara.msg.sys.pubsub.ProxyDriverSetup;
import org.jlab.clara.msg.sys.regdis.RegDriver;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

class ConnectionManager {

    // Factory
    private final ConnectionFactory factory;

    // pool of proxy connections
    private final ConnectionPool<ProxyAddress, ProxyDriver> proxyConnections;

    // pool of registrar connections
    private final ConnectionPool<RegAddress, RegDriver> registrarConnections;

    // default connection option
    private final ProxyDriverSetup proxySetup;

    ConnectionManager(ConnectionFactory factory) {
        this(factory, ProxyDriverSetup.newBuilder().build());
    }

    ConnectionManager(ConnectionFactory factory, ProxyDriverSetup setup) {
        this.factory = factory;
        this.proxyConnections = new ConnectionPool<>();
        this.registrarConnections = new ConnectionPool<>();
        this.proxySetup = setup;
    }

    ProxyDriver createProxySubscriber(ProxyAddress address) throws ClaraMsgException {
        return factory.createSubscriberConnection(address, proxySetup);
    }

    ProxyDriver createProxyConnection(ProxyAddress address) throws ClaraMsgException {
        return factory.createPublisherConnection(address, proxySetup);
    }

    ProxyDriver getProxyConnection(ProxyAddress address) throws ClaraMsgException {
        ProxyDriver cachedConnection = proxyConnections.getConnection(address);
        if (cachedConnection != null) {
            return cachedConnection;
        }
        return createProxyConnection(address);
    }

    void releaseProxyConnection(ProxyDriver connection) {
        proxyConnections.setConnection(connection.getAddress(), connection);
    }

    RegDriver getRegistrarConnection(RegAddress address) throws ClaraMsgException {
        RegDriver cachedConnection = registrarConnections.getConnection(address);
        if (cachedConnection != null) {
            return cachedConnection;
        }
        return factory.createRegistrarConnection(address);
    }

    void releaseRegistrarConnection(RegDriver connection) {
        registrarConnections.setConnection(connection.getAddress(), connection);
    }

    void destroy(int linger) {
        proxyConnections.destroyAll(c -> c.close(linger));
        registrarConnections.destroyAll(c -> c.close());
    }


    static class ConnectionPool<A, C> {
        private final Map<A, Queue<C>> connections = new ConcurrentHashMap<>();

        public C getConnection(A address) {
            Queue<C> cache = connections.get(address);
            if (cache != null) {
                return cache.poll();
            }
            return null;
        }

        public void setConnection(A address, C connection) {
            Queue<C> cache = connections.get(address);
            if (cache == null) {
                cache = new ConcurrentLinkedQueue<>();
                Queue<C> tempCache = connections.putIfAbsent(address, cache);
                if (tempCache != null) {
                    cache = tempCache;
                }
            }
            cache.add(connection);
        }

        public void destroyAll(Consumer<C> destroy) {
            for (Map.Entry<A, Queue<C>> cache : connections.entrySet()) {
                cache.getValue().forEach(destroy);
            }
        }
    }
}
