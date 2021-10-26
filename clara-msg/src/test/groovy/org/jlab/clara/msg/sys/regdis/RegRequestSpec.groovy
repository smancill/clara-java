/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.regdis

import com.google.protobuf.InvalidProtocolBufferException
import org.jlab.clara.msg.core.Topic
import org.jlab.clara.msg.data.RegDataProto.RegData
import org.jlab.clara.msg.errors.ClaraMsgException
import org.zeromq.ZMsg
import spock.lang.Shared
import spock.lang.Specification

import static org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType.SUBSCRIBER
import static org.jlab.clara.msg.sys.regdis.RegFactory.newRegistration

class RegRequestSpec extends Specification {

    @Shared RegData regData

    void setupSpec() {
        var topic = Topic.wrap("writer.scifi:books")
        regData = newRegistration("asimov", "10.2.9.1", SUBSCRIBER, topic).build()
    }

    def "Send and parse a registration request with protobuf registration data"() {
        given: "a request with protobuf registration data"
        var sendRequest = new RegRequest("foo:bar", "foo_service", regData)

        when: "parsing the request from the ZMQ raw message"
        var recvRequest = new RegRequest(sendRequest.msg())

        then: "all values are parsed correctly"
        with(recvRequest) {
            topic() == "foo:bar"
            sender() == "foo_service"
            data() == regData
        }
    }

    def "Send and parse a registration request with string data"() {
        given: "a request with string data"
        var sendRequest = new RegRequest("foo:bar", "foo_service", "10.2.9.2")

        when: "parsing the request from the ZMQ raw message"
        var recvRequest = new RegRequest(sendRequest.msg())

        then: "all values are parsed correctly"
        with(recvRequest) {
            topic() == "foo:bar"
            sender() == "foo_service"
            text() == "10.2.9.2"
        }
    }

    def "Parsing a request from a malformed ZMQ message throws an exception"() {
        given: "a ZMQ message without the right number of parts"
        var msg = new ZMsg().tap {
            addString "foo:bar"
            addString "foo_service"
        }

        when: "parsing the request from the malformed message"
        new RegRequest(msg)

        then: "an invalid request exception is thrown"
        thrown ClaraMsgException
    }

    def "Parsing the request data from a ZMQ message with invalid Protobuf throws an exception"() {
        given: "a raw ZMQ message with invalid Protobuf data"
        var invalidData = regData.toByteArray()[0..-10] as byte[]

        var msg = new ZMsg().tap {
            addString "foo:bar"
            addString "foo_service"
            add invalidData
        }

        when: "parsing the data of the malformed request message"
        new RegRequest(msg).data()

        then: "an invalid Protobuf exception is thrown"
        thrown InvalidProtocolBufferException
    }
}
