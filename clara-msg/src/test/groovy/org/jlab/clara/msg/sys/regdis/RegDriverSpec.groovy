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

package org.jlab.clara.msg.sys.regdis

import org.jlab.clara.msg.core.Topic
import org.jlab.clara.msg.data.RegDataProto.RegData
import org.jlab.clara.msg.net.RegAddress
import org.jlab.clara.msg.net.SocketFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import static org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType.PUBLISHER
import static org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType.SUBSCRIBER
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
            verifyRequest(request, data, REGISTRATION_TIMEOUT)
        }

        where:
        type        || request
        PUBLISHER   || REGISTER_PUBLISHER
        SUBSCRIBER  || REGISTER_SUBSCRIBER
    }

    def "Send proper request to remove a registered #type actor"() {
        given:
        var data = regData("bradbury", type, topic)

        when:
        driver.removeRegistration(sender, data)

        then:
        interaction {
            verifyRequest(request, data, REGISTRATION_TIMEOUT)
        }

        where:
        type        || request
        PUBLISHER   || REMOVE_PUBLISHER
        SUBSCRIBER  || REMOVE_SUBSCRIBER
    }

    def "Send proper request to remove all registered actors from given host"() {
        when:
        driver.removeAllRegistration(sender, "10.2.9.1")

        then:
        interaction {
            verifyRequest(REMOVE_ALL_REGISTRATION, "10.2.9.1", REGISTRATION_TIMEOUT)
        }
    }

    def "Send proper discovery request to find a #type actor"() {
        given:
        var data = regData("bradbury", type, topic)

        when:
        driver.findRegistration(sender, data)

        then:
        interaction {
            verifyRequest(request, data, DISCOVERY_TIMEOUT)
        }

        where:
        type        || request
        PUBLISHER   || FIND_PUBLISHER
        SUBSCRIBER  || FIND_SUBSCRIBER
    }

    def "Send proper discovery request to filter #type actors"() {
        given:
        var data = regData("", type, "writer")

        when:
        driver.filterRegistration(sender, data)

        then:
        interaction {
            verifyRequest(request, data, DISCOVERY_TIMEOUT)
        }

        where:
        type        || request
        PUBLISHER   || FILTER_PUBLISHER
        SUBSCRIBER  || FILTER_SUBSCRIBER
    }

    def "Send proper discovery request to get all #type actors"() {
        given:
        var data = regData("", type, "*")

        when:
        driver.allRegistration(sender, data)

        then:
        interaction {
            verifyRequest(request, data, DISCOVERY_TIMEOUT)
        }

        where:
        type        || request
        PUBLISHER   || ALL_PUBLISHER
        SUBSCRIBER  || ALL_SUBSCRIBER
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
        type        | _
        PUBLISHER   | _
        SUBSCRIBER  | _
    }

    private static RegData regData(String name, RegData.OwnerType type, String topic) {
        return RegFactory.newRegistration(name, "10.0.0.1", type, Topic.wrap(topic)).build()
    }

    private void verifyRequest(String topic, var data, int timeout) {
        1 * driver.request(_ as RegRequest, _ as Long) >> { requestArg, timeoutArg ->
            assert requestArg == new RegRequest(topic, sender, data)
            assert timeoutArg == timeout
            return new RegResponse(topic, sender)
        }
    }
}
