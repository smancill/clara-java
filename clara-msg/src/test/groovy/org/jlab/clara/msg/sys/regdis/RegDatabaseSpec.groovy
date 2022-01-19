/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis

import org.jlab.clara.msg.core.Topic
import org.jlab.clara.msg.data.RegDataProto.RegData
import org.jlab.clara.msg.sys.regdis.RegDatabase.TopicMatch

import spock.lang.Rollup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class RegDatabaseSpec extends Specification {

    static final RegData.Type TYPE = RegData.Type.PUBLISHER

    @Shared RegData asimov1
    @Shared RegData bradbury1
    @Shared RegData asimov2
    @Shared RegData bradbury2
    @Shared RegData twain1
    @Shared RegData twain2
    @Shared RegData brando2
    @Shared RegData tolkien1

    @Subject RegDatabase db

    void setupSpec() {
        // first test topic, four actors on two hosts
        asimov1 = regData("asimov", "10.2.9.1", "writer:scifi:books")
        asimov2 = regData("asimov", "10.2.9.2", "writer:scifi:books")
        bradbury1 = regData("bradbury", "10.2.9.1", "writer:scifi:books")
        bradbury2 = regData("bradbury", "10.2.9.2", "writer:scifi:books")

        // second test topic, two actors on two hosts
        twain1 = regData("twain", "10.2.9.1", "writer:adventure")
        twain2 = regData("twain", "10.2.9.2", "writer:adventure")

        // third test topic, one actor on second host
        brando2 = regData("brando", "10.2.9.2", "actor")

        // fourth test topic, one actor on first host
        tolkien1 = regData("tolkien", "10.2.9.1", "writer:adventure:tales")
    }

    void setup() {
        db = new RegDatabase()
    }

    private void register(RegData... regs) {
        regs.each { db.register it }
    }

    private void registerAll() {
        register(asimov1, asimov2, bradbury1, bradbury2)
        register(twain1, twain2)
        register(brando2)
        register(tolkien1)
    }

    private void remove(RegData... regs) {
        regs.each { db.remove it }
    }

    def "A new registration database is created empty"() {
        expect:
        db.topics().empty
        db.all().empty
    }

    def "Registering the first actor in an empty database"() {
        when: "registering the first actor"
        register(asimov1)

        then: "the topic is added to the database"
        db.topics() == setOf("writer:scifi:books")

        and: "the actor is added to the database"
        db.get("writer:scifi:books") == setOf(asimov1)
    }

    def "Registering new actors to the only registered topic"() {
        given: "only one topic with registered actors"
        register(twain1)

        when: "registering a new actor to the topic"
        register(twain2)

        then: "the actor is added to the database"
        db.get("writer:adventure") == setOf(twain1, twain2)
    }

    def "Registering the first actors of new topics"() {
        given: "a topic with registered actors"
        register(asimov1, bradbury1)

        when: "registering actors to new topics"
        register(twain1)
        register(tolkien1)

        then: "the new topics are added to the database"
        db.topics() == setOf("writer:scifi:books", "writer:adventure", "writer:adventure:tales")

        and: "the new actors are added to the database"
        db.get("writer:scifi:books") == setOf(asimov1, bradbury1)
        db.get("writer:adventure") == setOf(twain1)
        db.get("writer:adventure:tales") == setOf(tolkien1)
    }

    def "Registering new actors to existing topics"() {
        given: "a few topics with registered actors"
        register(asimov1)
        register(twain1)

        when: "registering a new actor to an existing topic"
        register(twain2)

        then: "the actor is added to the database"
        db.get("writer:scifi:books") == setOf(asimov1)
        db.get("writer:adventure") == setOf(twain1, twain2)
    }

    def "Registering an already registered actor does not change the database"() {
        given: "some registered actors"
        register(asimov1, bradbury1)

        when: "trying to register an already registered actor"
        register(bradbury1)

        then: "the actor is not duplicated in the database"
        db.get("writer:scifi:books") == old(db.get("writer:scifi:books"))
    }

    def "Removing the only registered actor"() {
        given: "a database with a single registered actor"
        register(asimov1)

        when: "removing the actor"
        remove(asimov1)

        then: "the topic is removed from the database"
        db.topics().empty

        and: "the actor is removed from the database"
        db.get("writer:scifi:books").empty
    }

    def "Removing an actor of the only registered topic"() {
        given: "a database with just one topic with registered actors"
        register(asimov1, asimov2, bradbury1)

        when: "removing one of the actors"
        remove(asimov2)

        then: "the topic is still registered in the database"
        db.topics() == setOf("writer:scifi:books")

        and: "the actor is removed from the database"
        db.get("writer:scifi:books") == setOf(asimov1, bradbury1)
    }

    def "Removing the only registered actor of a topic"() {
        given: "one of the topics with just one registered actor"
        register(asimov1)
        register(twain1, twain2)

        when: "removing the actor of that topic"
        remove(asimov1)

        then: "the topic is removed from the database"
        db.topics() == setOf("writer:adventure")

        and: "the actor is removed from the database"
        db.get("writer:scifi:books").empty
        db.get("writer:adventure") == setOf(twain1, twain2)
    }

    def "Removing an actor of a topic with multiple registered actors"() {
        given: "a topic with multiple registered actors"
        register(asimov1, asimov2, bradbury1)
        register(twain1, twain2)

        when: "removing one of the actors of that topic"
        remove(bradbury1)

        then: "the topic is still registered in the database"
        db.topics() == setOf("writer:scifi:books", "writer:adventure")

        and: "the actor is removed from the database"
        db.get("writer:scifi:books") == setOf(asimov1, asimov2)
        db.get("writer:adventure") == setOf(twain1, twain2)
    }

    def "Removing an actor that is not registered does not change the database"() {
        given: "some registered actors"
        register(asimov1, asimov2)

        when: "trying to remove an actor that is not registered"
        remove(bradbury1)

        then: "the database is not changed"
        db.get("writer:scifi:books") == old(db.get("writer:scifi:books"))
    }

    def "Removing all actors of a given host"() {
        given: "a database with registered actors from multiple hosts"
        register(asimov1, asimov2, bradbury1, bradbury2)
        register(twain1, twain2)
        register(tolkien1)

        register(brando2)

        when: "removing all actors of a given host"
        db.remove("10.2.9.2")

        then: "the topics that only had actors of the given host are removed from the database"
        db.topics() == setOf("writer:scifi:books", "writer:adventure", "writer:adventure:tales")

        and: "all the actors of the given host are removed from the database"
        !("10.2.9.2" in db.all().host)
    }

    def "Removing all actors of the only host the actors are registered from"() {
        given: "a database with registered actors from one single host"
        register(asimov1, tolkien1)

        when: "removing all actors of the host"
        db.remove("10.2.9.1")

        then: "the database is left empty"
        db.topics().empty
        db.all().empty
    }

    @Rollup
    def "Finding registered actors by topic"() {
        given:
        register(asimov1, bradbury2, brando2, tolkien1)

        expect: "all actors that are matched by the topic"
        db.find(Topic.wrap(topic), TopicMatch.PREFIX_MATCHING) == matched

        where:
        topic                       || matched
        "writer"                    || setOf(asimov1, bradbury2, tolkien1)
        "actor"                     || setOf(brando2)

        "writer:scifi"              || setOf(asimov1, bradbury2)
        "writer:adventure"          || setOf(tolkien1)
        "actor:romance"             || [] as Set

        "writer:scifi:books"        || setOf(asimov1, bradbury2)
        "writer:adventure:tales"    || setOf(tolkien1)
        "actor:drama:movies"        || [] as Set

        "player"                    || [] as Set
        "actor:drama"               || [] as Set
        "writer:adventure:books"    || [] as Set
    }

    @Rollup
    def "Reverse-finding registered actors by topic"() {
        given:
        register(asimov1, bradbury2, twain1, twain2, brando2, tolkien1)

        expect: "all the actors the topic is matched by"
        db.find(Topic.wrap(topic), TopicMatch.REVERSE_MATCHING) == matched

        where:
        topic                       || matched
        "writer"                    || [] as Set
        "actor"                     || setOf(brando2)

        "writer:scifi"              || [] as Set
        "writer:adventure"          || setOf(twain1, twain2)
        "actor:drama"               || setOf(brando2)

        "writer:scifi:books"        || setOf(asimov1, bradbury2)
        "writer:adventure:tales"    || setOf(twain1, twain2, tolkien1)
        "actor:drama:movies"        || setOf(brando2)

        "player"                    || [] as Set
        "writer:children"           || [] as Set
        "writer:scifi:comics"       || [] as Set
    }

    @Rollup
    def "Filtering registered actors by specific filters"() {
        given:
        registerAll()

        expect: "all registered actors that match the given filter"
        db.filter(filter) == matched

        where:
        filter                                  || matched
        regFilter { topic = "writer" }          || setOf(asimov1, asimov2, bradbury1, bradbury2,
                                                         twain1, twain2, tolkien1)
        regFilter { host = "10.2.9.2" }         || setOf(asimov2, bradbury2, brando2, twain2)

        regFilter { topic = "artist" }          || [] as Set
        regFilter { host = "10.2.9.3" }         || [] as Set
    }

    @Rollup
    def "Get registered actors with the exact same topic"() {
        given:
        register(asimov1, bradbury2, brando2, twain1, twain2, tolkien1)

        expect:
        db.find(Topic.wrap(topic), TopicMatch.EXACT) == matched

        where:
        topic                   || matched
        "actor"                 || setOf(brando2)
        "writer:adventure"      || setOf(twain1, twain2)
        "writer:scifi:books"    || setOf(asimov1, bradbury2)

        "writer"                || [] as Set
        "writer:scifi"          || [] as Set
        "writer:scifi:comics"   || [] as Set
    }

    def "Get all registered actors in the database"() {
        given:
        registerAll()

        expect:
        db.all() == setOf(asimov1, asimov2, bradbury1, bradbury2,
                          twain1, twain2, brando2, tolkien1)
    }

    private static RegData regData(String name, String host, String topic) {
        RegFactory.newRegistration(name, host, TYPE, Topic.wrap(topic))
    }

    private static RegData regFilter(builder) {
        RegFactory.newFilter(TYPE).tap(builder).build()
    }

    private static Set<Topic> setOf(String... topics) {
        topics.collect { Topic.wrap it }
    }

    private static Set<RegData> setOf(RegData... regs) {
        regs.collect()
    }
}
