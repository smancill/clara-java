/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base;

import org.jlab.clara.msg.net.ProxyAddress;

/**
 * The address where a CLARA component is listening messages.
 */
public class ClaraAddress {

    private final ProxyAddress address;

    ClaraAddress(String host) {
        this.address = new ProxyAddress(host);
    }

    ClaraAddress(String host, int port) {
        this.address = new ProxyAddress(host, port);
    }

    /**
     * Returns the host address.
     *
     * @return the host IP address
     */
    public String host() {
        return address.host();
    }

    /**
     * Returns the port number.
     *
     * @return the port
     */
    public int port() {
        return address.pubPort();
    }

    ProxyAddress proxyAddress() {
        return address;
    }

    @Override
    public String toString() {
        return address.host() + ":" + address.pubPort();
    }

    @Override
    public int hashCode() {
        return 31 * address.hashCode();
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
        ClaraAddress other = (ClaraAddress) obj;
        return address.equals(other.address);
    }
}
