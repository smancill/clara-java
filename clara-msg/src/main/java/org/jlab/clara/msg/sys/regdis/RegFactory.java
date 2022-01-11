/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis;

import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.net.ProxyAddress;

/**
 * Ensures the egistration requests are properly constructed.
 */
public final class RegFactory {

    private RegFactory() { }

    /**
     * Creates the data for a registration request.
     *
     * @param name the name of the actor
     * @param host the host of the actor
     * @param type the type of the actor (publisher or subscriber)
     * @param topic the topic to be registered
     * @return the registration data
     */
    public static RegData newRegistration(String name,
                                          String host,
                                          RegData.OwnerType type,
                                          Topic topic) {
        return newRegistration(name, "", new ProxyAddress(host), type, topic);
    }

    /**
     * Creates the data for a registration request.
     *
     * @param name the name of the actor
     * @param address the address of the actor
     * @param type the type of the actor (publisher or subscriber)
     * @param topic the topic to be registered
     * @return the registration data
     */
    public static RegData newRegistration(String name,
                                          String description,
                                          ProxyAddress address,
                                          RegData.OwnerType type,
                                          Topic topic) {
        RegData.Builder regb = RegData.newBuilder();
        regb.setName(name);
        regb.setDescription(description);
        regb.setHost(address.host());
        regb.setPort(address.pubPort());
        regb.setDomain(topic.domain());
        regb.setSubject(topic.subject());
        regb.setType(topic.type());
        regb.setOwnerType(type);
        return regb.build();
    }

    /**
     * Creates an empty filter request.
     * The terms to filter the actors need to be set.
     *
     * @param type the type of the actor (publisher or subscriber)
     * @return the registration data to filter actors
     */
    public static RegData.Builder newFilter(RegData.OwnerType type) {
        RegData.Builder filter = RegData.newBuilder();
        filter.setName(RegConstants.UNDEFINED);
        filter.setHost(RegConstants.UNDEFINED);
        filter.setPort(0);
        filter.setDomain(Topic.ANY);
        filter.setSubject(Topic.ANY);
        filter.setType(Topic.ANY);
        filter.setOwnerType(type);
        return filter;
    }
}
