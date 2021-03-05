/*
 * Copyright (C) 2019. Jefferson Lab (JLAB). All Rights Reserved.
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

package org.jlab.clara.msg.core

import org.jlab.clara.msg.data.RegDataProto.RegData
import org.jlab.clara.msg.data.RegInfo
import org.jlab.clara.msg.data.RegQuery
import org.jlab.clara.msg.net.ProxyAddress
import org.jlab.clara.msg.net.RegAddress
import org.jlab.clara.msg.sys.ConnectionFactory
import org.jlab.clara.msg.sys.regdis.RegDriver
import org.jlab.clara.msg.sys.regdis.RegFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import static org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType.PUBLISHER
import static org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType.SUBSCRIBER

class ActorSpec extends Specification {

    static final Set<RegData> EMPTY_REG = []

    @Shared String name = "asimov"
    @Shared Topic topic = Topic.wrap("writer:scifi:book")

    @Shared ProxyAddress localAddr = new ProxyAddress("10.0.0.5")
    @Shared RegAddress regAddr = new RegAddress("10.0.0.1")

    RegDriver driver

    @Subject Actor actor

    def setup() {
        var setup = ActorSetup.newBuilder()
            .withRegistrar(regAddr)
            .withProxy(localAddr)
            .withPoolSize(1)
            .build()

        driver = Mock(RegDriver) {
            getAddress() >> regAddr
        }

        var connectionFactory = Stub(ConnectionFactory) {
            getContext() >> null
            createRegistrarConnection(_) >> driver
        }

        actor = new Actor(name, setup, connectionFactory)
    }

    def "Send registration request to register a #type actor with the registrar"() {
        given:
        RegInfo info = factory(topic, "desc")

        when:
        actor.register(info, regAddr, 1000)

        then:
        1 * driver.addRegistration(name, regOf(type, topic, "desc"), 1000L)

        where:
        factory             | type
        RegInfo::publisher  | PUBLISHER
        RegInfo::subscriber | SUBSCRIBER
    }

    def "Send registration request to remove a #type actor from the registrar"() {
        given:
        RegInfo info = factory(topic)

        when:
        actor.deregister(info, regAddr, 1500)

        then:
        1 * driver.removeRegistration(name, regOf(type, topic), 1500L)

        where:
        factory             | type
        RegInfo::publisher  | PUBLISHER
        RegInfo::subscriber | SUBSCRIBER
    }

    def "Send discovery request to find #type actors"() {
        given:
        RegQuery query = factory().matching(topic)

        when:
        actor.discover(query, regAddr, 2000)

        then:
        1 * driver.findRegistration(name, query.data().build(), 2000) >> EMPTY_REG

        where:
        factory                 | type
        RegQuery::publishers    | PUBLISHER
        RegQuery::subscribers   | SUBSCRIBER
    }

    def "Send discovery request to filter #type actors"() {
        given:
        RegQuery query = factory().withDomain("domain")

        when:
        actor.discover(query, regAddr, 2000)

        then:
        1 * driver.filterRegistration(name, query.data().build(), 2000) >> EMPTY_REG

        where:
        factory                 | type
        RegQuery::publishers    | PUBLISHER
        RegQuery::subscribers   | SUBSCRIBER
    }

    def "Send discovery request to get all #type actors"() {
        given:
        RegQuery query = factory().all()

        when:
        actor.discover(query, regAddr, 2000)

        then:
        1 * driver.allRegistration(name, query.data().build(), 2000) >> EMPTY_REG

        where:
        factory                 | type
        RegQuery::publishers    | PUBLISHER
        RegQuery::subscribers   | SUBSCRIBER
    }

    private RegData regOf(RegData.OwnerType regType, Topic topic, String description = null) {
        var builder = RegFactory.newRegistration(name, localAddr.host(), regType, topic)
        if (description != null) {
            builder.description = description
        }
        return builder.build()
    }
}
