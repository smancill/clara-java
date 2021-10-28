/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.net;

import java.io.UncheckedIOException;

/**
 * Proxy address.
 */
public class ProxyAddress {

    /** The default proxy server port. */
    public static final int DEFAULT_PORT = 7771;

    private final String host;
    private final int pubPort;
    private final int subPort;

    /**
     * Creates an address with default host and ports.
     *
     * @throws UncheckedIOException if the IP address of the host could not be resolved
     */
    public ProxyAddress() {
        this("localhost", DEFAULT_PORT);
    }

    /**
     * Creates an address with provided host and default ports.
     *
     * @param host the host IP address
     * @throws UncheckedIOException if the IP address of the host could not be resolved
     */
    public ProxyAddress(String host) {
        this(host, DEFAULT_PORT);
    }

    /**
     * Creates an address using provided host and publication port.
     * Subscription port is the publication port plus one.
     *
     * @param host the host address
     * @param port the publication port number
     * @throws UncheckedIOException if the IP address of the host could not be resolved
     */
    public ProxyAddress(String host, int port) {
        if (host == null) {
            throw new IllegalArgumentException("null IP address");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("invalid port: " + port);
        }
        this.host = AddressUtils.toHostAddress(host);
        this.pubPort = port;
        this.subPort = port + 1;
    }

    /**
     * Returns the host address.
     *
     * @return the host IP address
     */
    public String host() {
        return host;
    }

    /**
     * Returns the publication port number.
     *
     * @return the PUB port
     */
    public int pubPort() {
        return pubPort;
    }

    /**
     * Returns the subscription port number.
     *
     * @return the SUB port
     */
    public int subPort() {
        return subPort;
    }

    @Override
    public String toString() {
        return host + ":" + pubPort;
    }

    @Override
    public int hashCode() {
        return 31 * host.hashCode() + pubPort;
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
        ProxyAddress other = (ProxyAddress) obj;
        return host.equals(other.host) && pubPort == other.pubPort;
    }
}
