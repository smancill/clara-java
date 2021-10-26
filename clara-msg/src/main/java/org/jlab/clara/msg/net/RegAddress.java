/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.net;

import java.io.UncheckedIOException;

/**
 * Registrar address.
 */
public class RegAddress {

    /** The default registrar server port. */
    public static final int DEFAULT_PORT = 8888;

    private final String host;
    private final int port;

    /**
     * Creates an address with default host and port.
     *
     * @throws UncheckedIOException if the IP address of the host could not be resolved
     */
    public RegAddress() {
        this("localhost", DEFAULT_PORT);
    }

    /**
     * Creates an address with provided host and default port.
     *
     * @param host the host IP address
     * @throws UncheckedIOException if the IP address of the host could not be resolved
     */
    public RegAddress(String host) {
        this(host, DEFAULT_PORT);
    }

    /**
     * Creates an address using provided host and port.
     *
     * @param host the host address
     * @param port the port number
     * @throws UncheckedIOException if the IP address of the host could not be resolved
     */
    public RegAddress(String host, int port) {
        if (host == null) {
            throw new IllegalArgumentException("null IP address");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("invalid port: " + port);
        }
        this.host = AddressUtils.toHostAddress(host);
        this.port = port;
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
     * Returns the port number.
     *
     * @return the port
     */
    public int port() {
        return port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    @Override
    public int hashCode() {
        return 31 * host.hashCode() + port;
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
        RegAddress other = (RegAddress) obj;
        return host.equals(other.host) && port == other.port;
    }
}
