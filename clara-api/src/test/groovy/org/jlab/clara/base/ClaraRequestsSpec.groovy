/*
 * Copyright (c) 2016.  Jefferson Lab (JLab). All rights reserved.
 *
 * Permission to use, copy, modify, and distribute  this software and its
 * documentation for educational, research, and not-for-profit purposes,
 * without fee and without a signed licensing agreement.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING
 * OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY,
 * PROVIDED HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * This software was developed under the United States Government license.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
 */

package org.jlab.clara.base

import org.jlab.clara.base.ClaraRequests.BaseRequest
import org.jlab.clara.base.core.ClaraBase
import org.jlab.clara.base.core.ClaraComponent
import org.jlab.clara.base.error.ClaraException
import org.jlab.clara.msg.core.Message
import org.jlab.clara.msg.errors.ClaraMsgException
import spock.lang.Rollup
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ClaraRequestsSpec extends Specification {

    private static final ClaraComponent FRONT_END = ClaraComponent.dpe("10.2.9.1_java")
    private static final String TOPIC = "dpe:10.2.9.6_java"

    ClaraBase base = Mock(ClaraBase)
    MessageHandler handler = Mock(MessageHandler)

    @Subject
    BaseRequest request

    def setup() {
        request = new BaseRequest(base, FRONT_END, TOPIC) {
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
        1 * base.send(FRONT_END, msg)
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
        1 * base.syncSend(FRONT_END, msg, _)
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
