/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base

import org.jlab.clara.base.ClaraRequests.BaseRequest
import org.jlab.clara.base.core.ClaraBase
import org.jlab.clara.base.core.ClaraComponent
import org.jlab.clara.base.error.ClaraException
import org.jlab.clara.msg.core.Message
import org.jlab.clara.msg.errors.ClaraMsgException
import spock.lang.Rollup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ClaraRequestsSpec extends Specification {

    @Shared ClaraComponent frontEnd = ClaraComponent.dpe("10.2.9.1_java")
    @Shared String topic = "dpe:10.2.9.6_java"

    ClaraBase base = Mock(ClaraBase)
    MessageHandler handler = Mock(MessageHandler)

    @Subject
    BaseRequest request

    def setup() {
        request = new BaseRequest(base, frontEnd, topic) {
            @Override Message msg() { handler.msg()  }
            @Override String parseData(Message msg) { handler.parse(msg) }
        }
    }

    def "Request sends message to front-end"() {
        given:
        var msg = Stub(Message)

        when:
        request.run()

        then:
        1 * handler.msg() >> msg
        1 * base.send(frontEnd, msg)
    }

    def "Request throws on send failure"() {
        given:
        base.send(_, _) >> { throw new ClaraMsgException("send error") }

        when:
        request.run()

        then:
        thrown ClaraException
    }

    def "Request throws on message failure"() {
        given:
        handler.msg() >> { throw new ClaraException("invalid request") }

        when:
        request.run()

        then:
        thrown ClaraException
    }

    def "Sync request sends message to front-end"() {
        given:
        var msg = Stub(Message)

        when:
        request.syncRun(10, TimeUnit.SECONDS)

        then:
        1 * handler.msg() >> msg
        1 * base.syncSend(frontEnd, msg, _)
    }

    @Rollup
    def "Sync request is sent with timeout in millis"() {
        when:
        request.syncRun(value, unit)

        then:
        1 * base.syncSend(_, _, timeout)

        where:
        value | unit                  || timeout
        20    | TimeUnit.MILLISECONDS || 20L
        10    | TimeUnit.SECONDS      || 10_000L
    }

    def "Sync request parses response"() {
        given:
        var response = Stub(Message)

        when:
        var result = request.syncRun(10, TimeUnit.SECONDS)

        then:
        1 * base.syncSend(_, _, _) >> response
        1 * handler.parse(response) >> "test_response"

        result == "test_response"
    }

    def "Sync request throws on send failure"() {
        given:
        base.syncSend(_, _, _) >> { throw new ClaraException("send error") }

        when:
        request.syncRun(10, TimeUnit.SECONDS)

        then:
        thrown ClaraException
    }

    def "Sync request throws on message failure"() {
        given:
        handler.msg() >> { throw new ClaraException("invalid request") }

        when:
        request.syncRun(10, TimeUnit.SECONDS)

        then:
        thrown ClaraException
    }

    def "Sync request throws on response failure"() {
        given:
        handler.parse(_) >> { throw new ClaraException("invalid response") }

        when:
        request.syncRun(10, TimeUnit.SECONDS)

        then:
        thrown ClaraException
    }

    def "Sync request throws on timeout"() {
        given:
        base.syncSend(_, _, _) >> { throw new TimeoutException() }

        when:
        request.syncRun(2, TimeUnit.SECONDS)

        then:
        thrown TimeoutException
    }

    interface MessageHandler {
        Message msg() throws ClaraException
        String parse(Message) throws ClaraException
    }
}
