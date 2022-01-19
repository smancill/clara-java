/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis

import org.jlab.clara.msg.core.Topic
import org.jlab.clara.msg.data.RegDataProto.RegData
import org.jlab.clara.msg.net.RegAddress
import org.jlab.clara.msg.net.SocketFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import static org.jlab.clara.msg.data.RegDataProto.RegData.Type.PUBLISHER
import static org.jlab.clara.msg.data.RegDataProto.RegData.Type.SUBSCRIBER
import static org.jlab.clara.msg.sys.regdis.RegConstants.*  // codenarc-disable-line NoWildcardImports

class RegDriverSpec extends Specification {

    @Shared String topic = "writer:scifi:books"
    @Shared String sender = "testSender"

    @Subject RegDriver driver

    void setup() throws Exception {
        driver = Spy(constructorArgs: [new RegAddress("10.2.9.1"), Stub(SocketFactory)])
    }

    def "Send proper request to register a #type actor"() {
        given:
        var data = regData("bradbury", type, topic)

        when:
        driver.addRegistration(sender, data)

        then:
        interaction {
            verifyRequest(REGISTER, data, REGISTRATION_TIMEOUT)
        }

        where:
        type << [PUBLISHER, SUBSCRIBER]
    }

    def "Send proper request to remove a registered #type actor"() {
        given:
        var data = regData("bradbury", type, topic)

        when:
        driver.removeRegistration(sender, data)

        then:
        interaction {
            verifyRequest(REMOVE, data, REGISTRATION_TIMEOUT)
        }

        where:
        type << [PUBLISHER, SUBSCRIBER]
    }

    def "Send proper request to remove all registered actors from given host"() {
        when:
        driver.removeAllRegistration(sender, "10.2.9.1")

        then:
        interaction {
            verifyRequest(REMOVE_ALL, regFilter(PUBLISHER, "10.2.9.1"), REGISTRATION_TIMEOUT)
            verifyRequest(REMOVE_ALL, regFilter(SUBSCRIBER, "10.2.9.1"), REGISTRATION_TIMEOUT)
        }
    }

    def "Send proper discovery request to find a #type actor"() {
        given:
        var data = regData("bradbury", type, topic)

        when:
        driver.findRegistration(sender, data)

        then:
        interaction {
            verifyRequest(FIND_MATCHING, data, DISCOVERY_TIMEOUT)
        }

        where:
        type << [PUBLISHER, SUBSCRIBER]
    }

    def "Send proper discovery request to filter #type actors"() {
        given:
        var data = regData("", type, "writer")

        when:
        driver.filterRegistration(sender, data)

        then:
        interaction {
            verifyRequest(FILTER, data, DISCOVERY_TIMEOUT)
        }

        where:
        type << [PUBLISHER, SUBSCRIBER]
    }

    def "Send proper discovery request to get all #type actors"() {
        given:
        var data = regData("", type, "*")

        when:
        driver.allRegistration(sender, data)

        then:
        interaction {
            verifyRequest(FIND_ALL, data, DISCOVERY_TIMEOUT)
        }

        where:
        type << [PUBLISHER, SUBSCRIBER]
    }

    def "Get #type registration data from registrar server response"() {
        given:
        var query = regData("", type, topic)

        and:
        Set<RegData> regActors = [
            regData("bradbury1", type, topic),
            regData("bradbury2", type, topic),
        ]

        driver.request(_, _) >> new RegResponse(topic, sender, regActors)

        when:
        Set<RegData> response = driver.findRegistration(sender, query)

        then:
        response == regActors

        where:
        type << [PUBLISHER, SUBSCRIBER]
    }

    private static RegData regData(String name, RegData.Type type, String topic) {
        return RegFactory.newRegistration(name, "10.0.0.1", type, Topic.wrap(topic))
    }

    private static RegData regFilter(RegData.Type type, String host) {
        return RegFactory.newFilter(type).setHost(host).build()
    }

    private void verifyRequest(String topic, RegData data, int timeout) {
        1 * driver.request(_ as RegRequest, _ as Long) >> { requestArg, timeoutArg ->
            assert requestArg == new RegRequest(topic, sender, data)
            assert timeoutArg == timeout
            return new RegResponse(topic, sender)
        }
    }
}
