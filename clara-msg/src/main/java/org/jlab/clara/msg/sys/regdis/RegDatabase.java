/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis;

import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegDataProto.RegData;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A registration database of actors.
 * Actors are grouped by topic, i.e., actors registered with the same topic will
 * be in the same group.
 */
class RegDatabase {

    private final ConcurrentMap<String, ConcurrentMap<Topic, Set<RegData>>>
            db = new ConcurrentHashMap<>();


    enum TopicMatch {

        /**
         * Compare topics using default prefix matching.
         * <p>
         * If there are actors registered to these topics:
         * <ol>
         * <li>{@code "DOMAIN:SUBJECT:TYPE"}
         * <li>{@code "DOMAIN:SUBJECT"}
         * <li>{@code "DOMAIN"}
         * </ol>
         * then this will be matched for the following requested topics:
         * <pre>
         * "DOMAIN"               -->  1, 2, 3
         * "DOMAIN:SUBJECT"       -->  1, 2
         * "DOMAIN:SUBJECT:TYPE"  -->  1
         * </pre>
         */
        PREFIX_MATCHING((st, rt) -> st.isParent(rt)),

        /**
         * Compare topics using reversed prefix matching.
         * <p>
         * If there are actors registered to these topics:
         * <ol>
         * <li>{@code "DOMAIN:SUBJECT:TYPE"}
         * <li>{@code "DOMAIN:SUBJECT"}
         * <li>{@code "DOMAIN"}
         * </ol>
         * then this will be matched for the following requested topics:
         * <pre>
         * "DOMAIN"               -->  3
         * "DOMAIN:SUBJECT"       -->  3, 2
         * "DOMAIN:SUBJECT:TYPE"  -->  3, 2, 1
         * </pre>
         */
        REVERSE_MATCHING((st, rt) -> rt.isParent(st)),

        /**
         * Compare topics using equality. No prefix matching is done.
         * <p>
         * If there are actors registered to these topics:
         * <ol>
         * <li>{@code "DOMAIN:SUBJECT:TYPE"}
         * <li>{@code "DOMAIN:SUBJECT"}
         * <li>{@code "DOMAIN"}
         * </ol>
         * then this will be matched for the following requested topics:
         * <pre>
         * "DOMAIN"               -->  3
         * "DOMAIN:SUBJECT"       -->  2
         * "DOMAIN:SUBJECT:TYPE"  -->  1
         * </pre>
         */
        EXACT((st, rt) -> st.equals(rt));

        private final BiPredicate<Topic, Topic> compare;

        TopicMatch(BiPredicate<Topic, Topic> compare) {
            this.compare = compare;
        }

        boolean test(Topic requestedTopic, Topic registeredTopic) {
            return compare.test(requestedTopic, registeredTopic);
        }
    }


    /**
     * Adds a new actor to the registration.
     * The actor will be grouped along with all other actors with the same
     * topic. If the actor is already registered, nothing is changed.
     *
     * @param regData the description of the actor
     */
    public void register(RegData regData) {
        var topic = getTopic(regData);

        ConcurrentMap<Topic, Set<RegData>> regMap =
                db.computeIfAbsent(getIndex(topic), k -> new ConcurrentHashMap<>());

        regMap.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet())
              .add(regData);
    }


    /**
     * Removes the given actor from the registration.
     *
     * @param regData the description of the actor
     */
    public void remove(RegData regData) {
        var topic = getTopic(regData);

        ConcurrentMap<Topic, Set<RegData>> regMap = db.get(getIndex(topic));
        if (regMap == null) {
            return;
        }

        var regActors = regMap.get(topic);
        if (regActors != null) {
            regActors.removeIf(sameRegistration(regData));
            regMap.remove(topic, Set.of());
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
        for (ConcurrentMap<Topic, Set<RegData>> regMap : db.values()) {
            var modified = regMap.entrySet().stream()
                    .filter(e -> e.getValue().removeIf(sameHost(host)))
                    .map(e -> e.getKey())
                    .collect(Collectors.toSet());
            modified.forEach(k -> regMap.remove(k, Set.of()));
        }
    }


    /**
     * Returns a set with all actors whose topic is matched by the given topic.
     * Empty if no actor is found.
     * @param topic the searched topic
     * @param topicMatch the topic matching comparison
     * @return the set of all actors that are matched by the topic
     */
    public Set<RegData> find(Topic topic, TopicMatch topicMatch) {
        // Optimize the EXACT match case
        if (topicMatch == TopicMatch.EXACT) {
            return get(topic);
        }
        ConcurrentMap<Topic, Set<RegData>> regMap = db.get(getIndex(topic));
        if (regMap == null) {
            return Set.of();
        }
        return regMap.entrySet().stream()
                .filter(entry -> topicMatch.test(topic, entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
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
        var filter = new Filter(data);
        db.entrySet().stream()
                .filter(e -> filter.matchIndex(e.getKey()))
                .flatMap(e -> e.getValue().entrySet().stream())
                .forEach(e -> filter.filter(e.getKey(), e.getValue()));
        return filter.result();
    }


    /**
     * Returns all registered actors.
     *
     * @return the set of all actors
     */
    public Set<RegData> all() {
        return db.values().stream()
                .map(ConcurrentMap::values)
                .flatMap(Collection::stream)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }


    /**
     * Returns all registered topics.
     *
     * @see #get
     */
    public Set<Topic> topics() {
        return db.values().stream()
                .map(ConcurrentMap::keySet)
                .flatMap(Set::stream)
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
        ConcurrentMap<Topic, Set<RegData>> regMap = db.get(getIndex(topic));
        if (regMap == null) {
            return Set.of();
        }
        var result = regMap.get(topic);
        if (result == null) {
            return Set.of();
        }
        return result;
    }


    private static String getIndex(Topic topic) {
        return topic.domain();
    }


    private static Topic getTopic(RegData regData) {
        return Topic.build(regData.getDomain(), regData.getSubject(), regData.getType());
    }


    private static Predicate<RegData> sameRegistration(RegData data) {
        return r -> data.getName().equals(r.getName()) && data.getHost().equals(r.getHost());
    }


    private static Predicate<RegData> sameHost(String host) {
        return r -> r.getHost().equals(host);
    }


    private static final class Filter {

        private final Set<RegData> result = new HashSet<>();

        private final TopicFilter domain;
        private final TopicFilter subject;
        private final TopicFilter type;
        private final @Nullable String host;
        private final int port;

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

        private Filter(RegData data) {
            this.domain = new TopicFilter(data.getDomain());
            this.subject = new TopicFilter(data.getSubject());
            this.type = new TopicFilter(data.getType());
            this.host = data.getHost().equals(RegConstants.UNDEFINED) ? null : data.getHost();
            this.port = data.getPort();
        }

        public void filter(Topic topic, Set<RegData> actors) {
            if (matchTopic(topic)) {
                if (hasAddress()) {
                    actors.stream().filter(this::matchAddress).forEach(result::add);
                } else {
                    result.addAll(actors);
                }
            }
        }

        public Set<RegData> result() {
            return result;
        }

        public boolean matchIndex(String indexKey) {
            return domain.any || indexKey.equals(domain.value);
        }

        private boolean matchTopic(Topic topic) {
            return (domain.any  || topic.domain().equals(domain.value))
                && (subject.any || topic.subject().equals(subject.value))
                && (type.any    || topic.type().equals(type.value));
        }

        private boolean matchAddress(RegData actor) {
            return actor.getHost().equals(host)
                    && (port == 0 || actor.getPort() == port);
        }

        private boolean hasAddress() {
            return host != null;
        }
    }
}
