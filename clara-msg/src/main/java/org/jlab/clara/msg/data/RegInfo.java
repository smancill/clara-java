/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.data;

import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegDataProto.RegData;

/**
 * Defines the parameters to register an actor with the registrar service.
 */
public final class RegInfo {

    private final RegData.Type type;
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
        return new RegInfo(RegData.Type.PUBLISHER, topic, description);
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
        return new RegInfo(RegData.Type.SUBSCRIBER, topic, description);
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

    private RegInfo(RegData.Type type, Topic topic, String description) {
        this.type = type;
        this.topic = topic;
        this.description = description;
    }

    /**
     * Returns the type parameter (publisher or subscriber).
     *
     * @return the registration type
     */
    public RegData.Type type() {
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
