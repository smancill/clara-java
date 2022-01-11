/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis;

import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegDataProto.RegData;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * A registration database of actors.
 * Actors are grouped by topic, i.e., actors registered with the same topic will
 * be in the same group.
 */
class RegDatabase {

    private final ConcurrentMap<String, ConcurrentMap<Topic, Set<RegData>>>
            db = new ConcurrentHashMap<>();

    /**
     * Adds a new actor to the registration.
     * The actor will be grouped along with all other actors with the same
     * topic. If the actor is already registered, nothing is changed.
     *
     * @param regData the description of the actor
     */
    public void register(RegData regData) {
        ConcurrentMap<Topic, Set<RegData>> map =
                db.computeIfAbsent(regData.getDomain(), k -> new ConcurrentHashMap<>());
        Topic key = generateKey(regData);
        if (map.containsKey(key)) {
            map.get(key).add(regData);
        } else {
            Set<RegData> regSet = new HashSet<>();
            regSet.add(regData);
            map.put(key, regSet);
        }
    }


    /**
     * Removes the given actor from the registration.
     *
     * @param regData the description of the actor
     */
    public void remove(RegData regData) {
        ConcurrentMap<Topic, Set<RegData>> map = db.get(regData.getDomain());
        if (map == null) {
            return;
        }
        Topic key = generateKey(regData);
        if (map.containsKey(key)) {
            Set<RegData> set = map.get(key);
            set.removeIf(r -> r.getName().equals(regData.getName())
                           && r.getHost().equals(regData.getHost()));
            if (set.isEmpty()) {
                map.remove(key);
            }
        }
    }


    /**
     * Removes all actors on the given host from the registration.
     * Useful when a node will be shutdown, so all actors running in the
     * node have to be unregistered.
     *
     * @param host the host of the actors that should be removed
     */
    public void remove(String host) {
        for (ConcurrentMap<Topic, Set<RegData>> map : db.values()) {
            Iterator<Entry<Topic, Set<RegData>>> dbIt = map.entrySet().iterator();
            while (dbIt.hasNext()) {
                Set<RegData> regSet = dbIt.next().getValue();
                regSet.removeIf(reg -> reg.getHost().equals(host));
                if (regSet.isEmpty()) {
                    dbIt.remove();
                }
            }
        }
    }


    private Topic generateKey(RegData regData) {
        return Topic.build(regData.getDomain(), regData.getSubject(), regData.getType());
    }


    /**
     * Returns a set with all actors whose topic is matched by the given topic.
     * Empty if no actor is found.
     * <p>
     * The rules to match topics are the following.
     * If we have actors registered to these topics:
     * <ol>
     * <li>{@code "DOMAIN:SUBJECT:TYPE"}
     * <li>{@code "DOMAIN:SUBJECT"}
     * <li>{@code "DOMAIN"}
     * </ol>
     * then this will be returned:
     * <pre>
     * find("DOMAIN", "*", "*")           -->  1, 2, 3
     * find("DOMAIN", "SUBJECT", "*")     -->  1, 2
     * find("DOMAIN", "SUBJECT", "TYPE")  -->  1
     * </pre>
     *
     * @param domain the searched domain
     * @param subject the searched type (it can be undefined)
     * @param type the searched type (it can be undefined)
     * @return the set of all actors that are matched by the topic
     */
    public Set<RegData> find(String domain, String subject, String type) {
        Set<RegData> result = new HashSet<>();
        ConcurrentMap<Topic, Set<RegData>> map = db.get(domain);
        if (map == null) {
            return result;
        }
        Topic searchedTopic = Topic.build(domain, subject, type);
        for (Topic topic : map.keySet()) {
            if (searchedTopic.isParent(topic)) {
                result.addAll(map.get(topic));
            }
        }
        return result;
    }


    /**
     * Returns a set with all actors whose topic matches the given topic.
     * Empty if no actor is found.
     * <p>
     * The rules to match topics are the following.
     * If we have actors registered to these topics:
     * <ol>
     * <li>{@code "DOMAIN:SUBJECT:TYPE"}
     * <li>{@code "DOMAIN:SUBJECT"}
     * <li>{@code "DOMAIN"}
     * </ol>
     * then this will be returned:
     * <pre>
     * find("DOMAIN", "*", "*")           -->  3
     * find("DOMAIN", "SUBJECT", "*")     -->  3, 2
     * find("DOMAIN", "SUBJECT", "TYPE")  -->  3, 2, 1
     * </pre>
     *
     * @param domain the searched domain
     * @param subject the searched type (it can be undefined)
     * @param type the searched type (it can be undefined)
     * @return the set of all actors that match the topic
     */
    public Set<RegData> rfind(String domain, String subject, String type) {
        Set<RegData> result = new HashSet<>();
        ConcurrentMap<Topic, Set<RegData>> map = db.get(domain);
        if (map == null) {
            return result;
        }
        Topic searchedTopic = Topic.build(domain, subject, type);
        for (Topic topic : map.keySet()) {
            if (topic.isParent(searchedTopic)) {
                result.addAll(map.get(topic));
            }
        }
        return result;
    }

    /**
     * Returns a set with all actors whose registration exactly matches the
     * given terms. Empty if no actor is found.
     * <p>
     * The search terms can be:
     * <ul>
     * <li>domain
     * <li>subject
     * <li>type
     * <li>address
     * </ul>
     * Only defined terms will be used for matching actors.
     * The topic parts are undefined if its value is {@link Topic#ANY}.
     * The address is undefined if its value is {@link RegConstants#UNDEFINED}.
     *
     * @param data the searched terms
     * @return the set of all actors that match the terms
     */
    public Set<RegData> filter(RegData data) {
        Filter filter = new Filter(data);
        for (Entry<String, ConcurrentMap<Topic, Set<RegData>>> level : db.entrySet()) {
            if (!filter.matchDomain(level.getKey())) {
                continue;
            }
            for (Entry<Topic, Set<RegData>> entry : level.getValue().entrySet()) {
                filter.filter(entry.getKey(), entry.getValue());
            }
        }
        return filter.result();
    }


    /**
     * Returns a set with all actors whose topic is the same as the given topic.
     * Empty if no actor is found.
     * <p>
     * The topics must be equals. No prefix matching is done.
     * If we have actors registered to these topics:
     * <ol>
     * <li>{@code "DOMAIN:SUBJECT:TYPE"}
     * <li>{@code "DOMAIN:SUBJECT"}
     * <li>{@code "DOMAIN"}
     * </ol>
     * then this will be returned:
     * <pre>
     * find("DOMAIN", "*", "*")           -->  3
     * find("DOMAIN", "SUBJECT", "*")     -->  2
     * find("DOMAIN", "SUBJECT", "TYPE")  -->  1
     * </pre>
     *
     * @param domain the searched domain
     * @param subject the searched type (it can be undefined)
     * @param type the searched type (it can be undefined)
     * @return the set of all actors that have the same topic
     */
    public Set<RegData> same(String domain, String subject, String type) {
        Set<RegData> result = new HashSet<>();
        ConcurrentMap<Topic, Set<RegData>> map = db.get(domain);
        if (map == null) {
            return result;
        }
        Topic searchedTopic = Topic.build(domain, subject, type);
        for (Topic topic : map.keySet()) {
            if (searchedTopic.equals(topic)) {
                result.addAll(map.get(topic));
            }
        }
        return result;
    }


    /**
     * Returns all registered actors.
     *
     * @return the set of all actors
     */
    public Set<RegData> all() {
        return db.values()
                 .stream()
                 .flatMap(m -> m.values().stream())
                 .flatMap(s -> s.stream())
                 .collect(Collectors.toSet());
    }


    /**
     * Returns all registered topics.
     *
     * @see #get
     */
    public Set<Topic> topics() {
        return db.values()
                 .stream()
                 .flatMap(m -> m.keySet().stream())
                 .collect(Collectors.toSet());
    }


    /**
     * Returns all actors registered with the specific known topic.
     *
     * @see #topics
     */
    public Set<RegData> get(String topic) {
        return get(Topic.wrap(topic));
    }


    /**
     * Returns all actors registered with the specific known topic.
     *
     * @see #topics
     */
    public Set<RegData> get(Topic topic) {
        ConcurrentMap<Topic, Set<RegData>> map = db.get(topic.domain());
        if (map == null) {
            return new HashSet<>();
        }
        Set<RegData> result = map.get(topic);
        if (result == null) {
            return new HashSet<>();
        }
        return result;
    }



    private static final class Filter {

        private final Set<RegData> result = new HashSet<>();

        private final TopicFilter domain;
        private final TopicFilter subject;
        private final TopicFilter type;
        private final AddressFilter address;

        /**
         * Cache a topic term.
         * Avoid checking if the value is any for every actor.
         */
        private static final class TopicFilter {
            private final boolean any;
            private final String value;

            private TopicFilter(String value) {
                this.any = value.equals(Topic.ANY);
                this.value = value;
            }
        }

        /**
         * Cache the address
         * Avoid checking if the address is set for every actor.
         */
        private static final class AddressFilter {
            private final boolean filter;
            private final String host;
            private final int port;

            private AddressFilter(RegData data) {
                this.host = data.getHost();
                this.port = data.getPort();
                this.filter = !host.equals(RegConstants.UNDEFINED);
            }
        }

        private Filter(RegData data) {
            this.domain = new TopicFilter(data.getDomain());
            this.subject = new TopicFilter(data.getSubject());
            this.type = new TopicFilter(data.getType());
            this.address = new AddressFilter(data);
        }

        public void filter(Topic topic, Set<RegData> actors) {
            if (matchTopic(topic)) {
                if (filterAddress()) {
                    actors.stream().filter(this::matchAddress).forEach(result::add);
                } else {
                    result.addAll(actors);
                }
            }
        }

        private boolean filterAddress() {
            return address.filter;
        }

        public Set<RegData> result() {
            return result;
        }

        public boolean matchDomain(String regDomain) {
            return domain.any | regDomain.equals(domain.value);
        }

        public boolean matchTopic(Topic topic) {
            return (domain.any  || topic.domain().equals(domain.value))
                && (subject.any || topic.subject().equals(subject.value))
                && (type.any    || topic.type().equals(type.value));
        }

        public boolean matchAddress(RegData actor) {
            return actor.getHost().equals(address.host)
                    && (address.port == 0 || actor.getPort() == address.port);
        }
    }
}
