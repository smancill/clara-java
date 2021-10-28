/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

/**
 * The main identification for pub/sub communications.
 * Message are <b>topic-based</b>, this is, messages are published to given
 * "topics", or named channels, defined by <i>publishers</i>.
 * <i>Subscribers</i> can receive the messages published to the "topics" to
 * which they are interested, by subscribing to them.
 * <p>
 * A topic is composed of three parts: a <b>domain</b>, a
 * <b>subject</b> and a <b>type</b>. Each part is separated by a semicolon
 * character. The subject and the type can be omitted. Thus, the following
 * topics are all valid:
 * <ul>
 * <li>{@code "domain"}
 * <li>{@code "domain:subject"}
 * <li>{@code "domain:subject:type"}
 * </ul>
 * The {@link #build} factory methods help to create a proper topic with the
 * right format.
 * <p>
 * When an {@link Actor actor} is subscribed to a given topic, it will only
 * receive messages published to that topic. To filter topics, the three  parts
 * form a hierarchy, and all topics with the same prefix will be accepted.
 * <p>
 * In other words, a subscriber listening for an specific "domain" will receive
 * all messages whose topic starts with that domain, no matter the subject and
 * the type. For example, if the subscription topic is {@code "A"}, then all the
 * messages with the following topics will be received:
 * <ul>
 * <li>{@code "A"}
 * <li>{@code "A:B"}
 * <li>{@code "A:C"}
 * <li>{@code "A:B:1"}
 * <li>{@code "A:C:1"}
 * <li>{@code "A:C:2"}
 * <li>etc...
 * </ul>
 * More specific subscriptions will not receive messages that match only the
 * parent parts of the topic.
 * Thus, subscription to {@code "A:B"} will accept {@code "A:B"}, {@code
 * "A:B:1"}, {@code "A:B:2"}, etc, but will reject {@code "A"} or {@code "A:C"}.
 * Similarly, a subscription to {@code "A:B:1"} will only accept that exact
 * topic, rejecting {@code "A:B:2"}, {@code "A:C"}, {@code "A"}, etc.
 */
public final class Topic {

    /**
     * A regex indicating to match any value.
     */
    public static final String ANY = "*";

    /**
     * The separator of the topic parts.
     */
    public static final String SEPARATOR = ":";

    private final String topic;


    /**
     * Construct a valid topic.
     */
    private Topic(String domain, String subject, String type) {
        if (domain == null || domain.equals(ANY)) {
            throw new IllegalArgumentException("domain is not defined");
        }
        StringBuilder topic = new StringBuilder();
        topic.append(domain);
        if (subject != null && !subject.equals(ANY)) {
            topic.append(SEPARATOR).append(subject);
            if (type != null && !type.equals(ANY)) {
                StringTokenizer st = new StringTokenizer(type, SEPARATOR);
                while (st.hasMoreTokens()) {
                    String tst = st.nextToken();
                    if (!tst.contains(ANY)) {
                        topic.append(SEPARATOR).append(tst);
                    } else {
                        break;
                    }
                }
            }
        }

        this.topic = topic.toString();
    }


    /**
     * Construct a topic from the given string.
     * The string must be a valid topic.
     */
    private Topic(String topic) {
        this.topic = topic;
    }

    /**
     * Builds a new topic with only a domain part.
     *
     * @param domain the domain of the topic
     * @return the created topic
     */
    public static Topic build(String domain) {
        return new Topic(domain, ANY, ANY);
    }

    /**
     * Builds a new topic with only domain and subject parts.
     *
     * @param domain the domain of the topic
     * @param subject the subject of the topic
     * @return the created topic
     */
    public static Topic build(String domain, String subject) {
        return new Topic(domain, subject, ANY);
    }

    /**
     * Builds a new full topic with domain, subject and type.
     *
     * @param domain the domain of the topic
     * @param subject the subject of the topic
     * @param type the type of the subject
     * @return the created topic
     */
    public static Topic build(String domain, String subject, String type) {
        return new Topic(domain, subject, type);
    }

    /**
     * Use the given string as a topic.
     * No validation is done to the string.
     * The caller must be sure it is a valid topic.
     * This factory method is provided for speed purposes.
     * It should be used with caution.
     *
     * @param topic a valid topic string
     * @return the created topic
     */
    public static Topic wrap(String topic) {
        return new Topic(topic);
    }

    /**
     * Use the given data as an topic.
     * This factory method is provided as a shortcut to get the topic from a 0MQ
     * frame.
     *
     * @param bytes binary representation of a valid topic
     */
    static Topic wrap(byte[] bytes) {
        return new Topic(new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Returns the domain part of the topic.
     *
     * @return the domain of the topic
     */
    public String domain() {
        int firstSep = topic.indexOf(SEPARATOR);
        if (firstSep < 0) {
            return topic;
        }
        return topic.substring(0, firstSep);
    }


    /**
     * Returns the subject part of the topic. If the topic has no subject, then
     * {@code "*"} is returned.
     *
     * @return the subject of the topic, of {@link #ANY} if not set
     */
    public String subject() {
        int firstSep = topic.indexOf(SEPARATOR);
        if (firstSep < 0) {
            return ANY;
        }
        int secondSep = topic.indexOf(SEPARATOR, firstSep + 1);
        if (secondSep < 0) {
            return topic.substring(firstSep + 1);
        }
        return topic.substring(firstSep + 1, secondSep);
    }


    /**
     * Returns the type part of the topic. If the topic has no type, then
     * {@code "*"} is returned.
     *
     * @return the type of the topic, of {@link #ANY} if not set
     */
    public String type() {
        int firstSep = topic.indexOf(SEPARATOR);
        if (firstSep < 0) {
            return ANY;
        }
        int secondSep = topic.indexOf(SEPARATOR, firstSep + 1);
        if (secondSep < 0) {
            return ANY;
        }
        return topic.substring(secondSep + 1);
    }


    /**
     * Returns true if this topic is a parent of the given topic.
     * A parent topic is a prefix of other topic, or they are the same.
     * Examples:
     * <ul>
     * <li>{@code "A"} is a parent of {@code "A:B"} and {@code "A:C:1"}
     * <li>{@code "A"} is NOT parent of {@code "W:B"} nor {@code "Z"}
     * <li>{@code "A:C"} is a parent of {@code "A:C:1"} and {@code "A:C"}
     * <li>{@code "A:C"} is NOT a parent of {@code "A:B"}
     * </ul>
     * A subscription to a parent topic will accept any children topic.
     * See the class documentation for more details about filtering messages by
     * topic.
     *
     * @param other the topic to match as a children
     * @return true if this topic is a parent of the other
     */
    public boolean isParent(Topic other) {
        String topic = other.toString();
        return topic.startsWith(this.topic);
    }


    @Override
    public String toString() {
        return topic;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + topic.hashCode();
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
        Topic other = (Topic) obj;
        return topic.equals(other.topic);
    }
}
