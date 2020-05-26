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

/**
 * Defines the parameters to register an actor with the registrar service.
 */
public final class RegInfo {

    private final RegData.OwnerType type;
    private final Topic topic;
    private final String description;

    /**
     * Describes a publication to the given topic.
     *
     * @param topic the topic to which messages are published
     * @param description general description of the published messages
     * @return the information required to (de)register an actor as a publisher
     */
    public static RegInfo publisher(Topic topic, String description) {
        return new RegInfo(RegData.OwnerType.PUBLISHER, topic, description);
    }

    /**
     * Describes a publication to the given topic.
     *
     * @param topic the topic to which messages are published
     * @return the information required to (de)register an actor as a publisher
     */
    public static RegInfo publisher(Topic topic) {
        return publisher(topic, "");
    }

    /**
     * Describes a subscription to the given topic.
     *
     * @param topic the topic of the subscription
     * @param description general description of the subscription
     * @return the information required to (de)register an actor as a subscriber
     */
    public static RegInfo subscriber(Topic topic, String description) {
        return new RegInfo(RegData.OwnerType.SUBSCRIBER, topic, description);
    }

    /**
     * Describes a subscription to the given topic.
     *
     * @param topic the topic of the subscription
     * @return the information required to (de)register an actor as a subscriber
     */
    public static RegInfo subscriber(Topic topic) {
        return subscriber(topic, "");
    }

    private RegInfo(RegData.OwnerType type, Topic topic, String description) {
        this.type = type;
        this.topic = topic;
        this.description = description;
    }

    /**
     * Returns the type parameter (publisher or subscriber).
     *
     * @return the registration type
     */
    public RegData.OwnerType type() {
        return type;
    }

    /**
     * Returns the topic parameter.
     *
     * @return the registration topic
     */
    public Topic topic() {
        return topic;
    }

    /**
     * Returns the description parameter.
     *
     * @return the registration description
     */
    public String description() {
        return description;
    }
}
