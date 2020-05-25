/*
 * Copyright (c) 2016.  Jefferson Lab (JLab). All rights reserved.
 *
 * Permission to use, copy, modify, and distribute  this software and its
 * documentation for educational, research, and not-for-profit purposes,
 * without fee and without a signed licensing agreement.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING
 * OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY,
 * PROVIDED HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * This software was developed under the United States Government license.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
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
