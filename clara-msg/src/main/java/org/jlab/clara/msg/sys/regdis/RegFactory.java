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
    public static RegData.Builder newRegistration(String name,
                                                  String host,
                                                  RegData.OwnerType type,
                                                  Topic topic) {
        return newRegistration(name, new ProxyAddress(host), type, topic);
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
    public static RegData.Builder newRegistration(String name,
                                                  ProxyAddress address,
                                                  RegData.OwnerType type,
                                                  Topic topic) {
        RegData.Builder regb = RegData.newBuilder();
        regb.setName(name);
        regb.setHost(address.host());
        regb.setPort(address.pubPort());
        regb.setDomain(topic.domain());
        regb.setSubject(topic.subject());
        regb.setType(topic.type());
        regb.setOwnerType(type);
        return regb;
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
