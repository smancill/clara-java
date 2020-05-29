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
import org.jlab.clara.msg.errors.ClaraMsgException
import org.zeromq.ZMsg
import spock.lang.Shared
import spock.lang.Specification

import static org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType.SUBSCRIBER
import static org.jlab.clara.msg.sys.regdis.RegFactory.newRegistration

class RegResponseSpec extends Specification {

    @Shared RegData regData1
    @Shared RegData regData2

    void setupSpec() {
        var topic = Topic.wrap("writer.scifi:books")
        regData1 = newRegistration("asimov", "10.2.9.1", SUBSCRIBER, topic).build()
        regData2 = newRegistration("bradbury", "10.2.9.1", SUBSCRIBER, topic).build()
    }

    def "Send and parse a success response for a registration request"() {
        given: "a success response"
        var sendResponse = new RegResponse("foo:bar", "registration_fe")

        when: "parsing the response from the ZMQ raw message"
        var recvResponse = new RegResponse(sendResponse.msg())

        then: "all values are parsed correctly"
        with(recvResponse) {
            topic() == "foo:bar"
            sender() == "registration_fe"
            status() == RegConstants.SUCCESS
            data().empty
        }
    }

    def "Send and parse an error response for a registration request"() {
        given: "an error response"
        var error = "could not handle request"
        var sendResponse = new RegResponse("foo:bar", "registration_fe", error)

        when: "parsing the response from the ZMQ raw message"
        new RegResponse(sendResponse.msg())

        then: "the error is wrapped into an exception and thrown"
        var ex = thrown(ClaraMsgException)
        ex.message =~ error
    }

    def "Create a response with registration data for a registration request"() {
        given: "a registration response with a set of registration data"
        var regDataSet = [regData1, regData2] as Set
        var sendResponse = new RegResponse("foo:bar", "registration_fe", regDataSet)

        when: "parsing the response from the ZMQ raw message"
        var recvResponse = new RegResponse(sendResponse.msg())

        then: "all values are parsed correctly"
        with(recvResponse) {
            topic() == "foo:bar"
            sender() == "registration_fe"
            status() == RegConstants.SUCCESS
            data() == regDataSet
        }
    }

    def "Parsing a response from a malformed ZMQ message throws an exception"() {
        given: "a ZMQ message without the right number of parts"
        var msg = new ZMsg().tap {
            addString "foo:bar"
            addString "foo_service"
        }

        when: "parsing the response from the malformed message"
        new RegResponse(msg)

        then: "an invalid response exception is thrown"
        var ex = thrown(ClaraMsgException)
        ex.message =~ "invalid registrar server response format"
    }

    def "Parsing the response from a ZMQ message with invalid Protobuf throws an exception"() {
        given: "a raw ZMQ message with invalid Protobuf data"
        var invalidData = regData2.toByteArray()[0..-10] as byte[]

        var msg = new ZMsg().tap {
            addString "foo:bar"
            addString "foo_service"
            addString RegConstants.SUCCESS
            add regData1.toByteArray()
            add invalidData
        }

        when: "parsing the data of the malformed response message"
        new RegResponse(msg)

        then: "an invalid response exception is thrown"
        var ex = thrown(ClaraMsgException)
        ex.message =~ "could not parse registrar server response"
    }
}
