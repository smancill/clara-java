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

import org.jlab.clara.base.ClaraSubscriptions.BaseSubscription
import org.jlab.clara.base.core.ClaraBase
import org.jlab.clara.base.core.ClaraComponent
import org.jlab.clara.base.error.ClaraException
import org.jlab.clara.msg.core.Callback
import org.jlab.clara.msg.core.Subscription
import org.jlab.clara.msg.core.Topic
import spock.lang.Specification

class ClaraSubscriptionsSpec extends Specification {

    private static final ClaraComponent FRONT_END = ClaraComponent.dpe("10.2.9.1_java")

    ClaraBase base = Mock(ClaraBase)
    CallbackWrapper wrapper = Mock(CallbackWrapper)
    EngineCallback callback = Stub(EngineCallback)

    Map<String, Subscription> subscriptions = [:]

    def "Start subscription uses front-end"() {
        when:
        makeSubscription("data:10.2.9.96_java:master:Simple").start(callback)

        then:
        1 * base.listen(FRONT_END, _, _)
    }

    def "Start subscription matches topic"() {
        when:
        makeSubscription("data:10.2.9.96_java:master:Simple").start(callback)

        then:
        1 * base.listen(_, Topic.wrap("data:10.2.9.96_java:master:Simple"), _)
    }

    def "Start subscription wraps user callback"() {
        given:
        var cb = Mock(Callback)

        when:
        makeSubscription("data:10.2.9.96_java:master:Simple").start(callback)

        then:
        1 * wrapper.wrap(callback) >> cb
        1 * base.listen(_, _, cb)
    }

    def "Start subscription throws on failure"() {
        given:
        base.listen(_, _, _) >> { throw new ClaraException("error") }

        when:
        makeSubscription("data:10.2.9.96_java:master:Simple").start(callback)

        then:
        thrown ClaraException
    }

    def "Start subscription stores subscription handler"() {
        given:
        var handler = Mock(Subscription)
        base.listen(_, _, _) >> handler

        when:
        makeSubscription("ERROR:10.2.9.96_java:master:Simple").start(callback)

        then:
        subscriptions["10.2.9.1#ERROR:10.2.9.96_java:master:Simple"] == handler
    }

    def "Start subscription throws on duplicated subscription"() {
        given:
        makeSubscription("data:10.2.9.96_java:master:Simple").start(callback)

        when:
        makeSubscription("data:10.2.9.96_java:master:Simple").start(callback)

        then:
        thrown IllegalStateException
    }

    def "Stop subscription uses handler"() {
        given:
        var handler = Mock(Subscription)
        base.listen(_, Topic.wrap("ERROR:10.2.9.96_java:master:Simple"), _) >> handler

        and:
        makeSubscription("ERROR:10.2.9.96_java:master:Simple").start(callback)
        makeSubscription("WARNING:10.2.9.96_java:master:Simple").start(callback)
        makeSubscription("INFO:10.2.9.96_java:master:Simple").start(callback)

        when:
        makeSubscription("ERROR:10.2.9.96_java:master:Simple").stop()

        then:
        1 * base.unsubscribe(handler)
    }

    def "Stop subscription removes subscription handler"() {
        given:
        makeSubscription("ERROR:10.2.9.96_java:master:Simple").start(callback)
        makeSubscription("WARNING:10.2.9.96_java:master:Simple").start(callback)
        makeSubscription("INFO:10.2.9.96_java:master:Simple").start(callback)

        when:
        makeSubscription("ERROR:10.2.9.96_java:master:Simple").stop()

        then:
        subscriptions.keySet() == [
            "10.2.9.1#WARNING:10.2.9.96_java:master:Simple",
            "10.2.9.1#INFO:10.2.9.96_java:master:Simple",
        ] as Set
    }

    private BaseSubscription makeSubscription(String topic) {
        return new BaseSubscription<BaseSubscription, EngineCallback>(
                base, subscriptions, FRONT_END, Topic.wrap(topic)
        ) {
            @Override
            protected Callback wrap(EngineCallback callback) {
                return wrapper.wrap(callback)
            }
        }
    }

    private interface CallbackWrapper {
        Callback wrap(EngineCallback)
    }
}
