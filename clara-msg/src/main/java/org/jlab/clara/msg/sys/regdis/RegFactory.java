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
        var reg = RegData.newBuilder();
        reg.setName(name);
        reg.setDescription(description);
        reg.setHost(address.host());
        reg.setPort(address.pubPort());
        reg.setDomain(topic.domain());
        reg.setSubject(topic.subject());
        reg.setType(topic.type());
        reg.setOwnerType(type);
        return reg.build();
    }

    /**
     * Creates an empty filter request.
     * The terms to filter the actors need to be set.
     *
     * @param type the type of the actor (publisher or subscriber)
     * @return the registration data to filter actors
     */
    public static RegData.Builder newFilter(RegData.OwnerType type) {
        var filter = RegData.newBuilder();
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
