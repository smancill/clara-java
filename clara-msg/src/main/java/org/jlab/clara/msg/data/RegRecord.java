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

package org.jlab.clara.msg.data;

import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.net.ProxyAddress;

/**
 * The registration information of an actor.
 * This is a wrapper over the protobuf registration data
 * with high-level methods to read the data.
 */
public class RegRecord {

    private final RegData data;

    /**
     * Creates a record wrapping the given registration data.
     *
     * @param regb the registration data
     */
    public RegRecord(RegData regb) {
        data = regb;
    }

    /**
     * Returns the name of the registered actor.
     *
     * @return the name of the actor
     */
    public String name() {
        return data.getName();
    }

    /**
     * Returns the type of the registered actor (a publisher or a subscriber).
     *
     * @return the type of the actor
     */
    public RegData.OwnerType type() {
        return data.getOwnerType();
    }

    /**
     * Returns the address to which the registered actor is connected.
     *
     * @return the address of the actor
     */
    public ProxyAddress address() {
        return new ProxyAddress(data.getHost(), data.getPort());
    }

    /**
     * Returns the topic of interest for the registered actor.
     *
     * @return the topic used by the actor
     */
    public Topic topic() {
        return Topic.build(data.getDomain(), data.getSubject(), data.getType());
    }

    /**
     * Returns a description of the registered actor.
     *
     * @return the description of the actor
     */
    public String description() {
        return data.getDescription();
    }

    /**
     * Returns the registration protobuf data.
     *
     * @return the wrapped registration data object.
     */
    public RegData data() {
        return data;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + data.hashCode();
        return result;
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
        RegRecord other = (RegRecord) obj;
        return data.equals(other.data);
    }
}
