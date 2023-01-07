/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
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
     * @param regData the registration data
     */
    public RegRecord(RegData regData) {
        data = regData;
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
    public RegData.Type type() {
        return data.getType();
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
        return Topic.wrap(data.getTopic());
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
