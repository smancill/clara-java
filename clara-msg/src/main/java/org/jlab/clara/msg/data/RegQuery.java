/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.data;

import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.net.AddressUtils;
import org.jlab.clara.msg.sys.regdis.RegFactory;

/**
 * Defines the parameters to search actors in the registrar service.
 */
public final class RegQuery {

    private final RegData data;
    private final Category category;

    /**
     * Creates a simple query to search publishers of the specified topic.
     *
     * @param topic the topic of interest
     * @return a query object
     */
    public static RegQuery publishers(Topic topic) {
        return publishers().matching(topic);
    }

    /**
     * Creates a factory of queries to search publishers.
     *
     * @return a queries factory
     */
    public static Factory publishers() {
        return new Factory(RegData.OwnerType.PUBLISHER);
    }

    /**
     * Creates a simple query to search subscribers of the specified topic.
     *
     * @param topic the topic of interest
     * @return a query object
     */
    public static RegQuery subscribers(Topic topic) {
        return subscribers().matching(topic);
    }

    /**
     * Creates a factory of queries to search subscribers.
     *
     * @return a queries factory
     */
    public static Factory subscribers() {
        return new Factory(RegData.OwnerType.SUBSCRIBER);
    }


    /**
     * A classification of registration queries.
     * Each category uses a different driver method.
     */
    public enum Category {
        /** Query the registrar by topic matching. */
        MATCHING,

        /** Query the registrar by comparing registration data. */
        FILTER,

        /** Query the registrar by exact topic. */
        EXACT,

        /** Query the registrar for all actors. */
        ALL
    }


    /**
     * Creates specific registration discovery queries.
     */
    public static final class Factory {

        private final RegData.Builder data;

        private Factory(RegData.OwnerType type) {
            data = RegFactory.newFilter(type);
        }

        /**
         * A query for registered actors matching the given topic.
         *
         * @param topic the topic to be matched
         * @return a query for actors with topics matching the given topic
         */
        public RegQuery matching(Topic topic) {
            data.setDomain(topic.domain());
            data.setSubject(topic.subject());
            data.setType(topic.type());
            return new RegQuery(data, Category.MATCHING);
        }

        /**
         * A query for registered actors with this exact domain.
         *
         * @param domain the expected domain
         * @return a query for actors registered to the given domain
         *         (subject and type are ignored)
         */
        public RegQuery withDomain(String domain) {
            data.setDomain(domain);
            return new RegQuery(data, Category.FILTER);
        }

        /**
         * A query for registered actors with this exact subject.
         *
         * @param subject the expected subject
         * @return a query for actors registered to the given subject
         *         (domain and type are ignored)
         */
        public RegQuery withSubject(String subject) {
            data.setSubject(subject);
            return new RegQuery(data, Category.FILTER);
        }

        /**
         * A query for registered actor with this exact type.
         *
         * @param type the expected type
         * @return a query for actors registered to the given type
         *         (domain and subject are ignored)
         */
        public RegQuery withType(String type) {
            data.setType(type);
            return new RegQuery(data, Category.FILTER);
        }

        /**
         * A query for registered actors with this exact topic.
         *
         * @param topic the topic to be compared
         * @return a query for actors with the same topic as the given topic
         */
        public RegQuery withSame(Topic topic) {
            data.setDomain(topic.domain());
            data.setSubject(topic.subject());
            data.setType(topic.type());
            return new RegQuery(data, Category.EXACT);
        }

        /**
         * A query for registered actors with this exact hostname.
         *
         * @param host the expected host
         * @return a query for actors registered from the given host
         */
        public RegQuery withHost(String host) {
            data.setHost(AddressUtils.toHostAddress(host));
            return new RegQuery(data, Category.FILTER);
        }

        /**
         * A query to get all registered actors.
         *
         * @return a query for all registered actors
         */
        public RegQuery all() {
            return new RegQuery(data, Category.ALL);
        }
    }


    private RegQuery(RegData.Builder data, Category category) {
        this.data = data.build();
        this.category = category;
    }

    /**
     * Serializes the query into a protobuf object.
     *
     * @return the query as a registration data object
     */
    public RegData data() {
        return data;
    }

    /**
     * The category of the query.
     *
     * @return the category
     */
    public Category category() {
        return category;
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
        RegQuery other = (RegQuery) obj;
        return data.equals(other.data);
    }
}
