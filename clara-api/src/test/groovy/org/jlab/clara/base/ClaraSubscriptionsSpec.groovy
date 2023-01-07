/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base

import org.jlab.clara.base.ClaraSubscriptions.BaseSubscription
import org.jlab.clara.base.core.ClaraBase
import org.jlab.clara.base.core.ClaraComponent
import org.jlab.clara.base.error.ClaraException
import org.jlab.clara.msg.core.Callback
import org.jlab.clara.msg.core.Subscription
import org.jlab.clara.msg.core.Topic
import spock.lang.Shared
import spock.lang.Specification

class ClaraSubscriptionsSpec extends Specification {

    @Shared ClaraComponent frontEnd = ClaraComponent.dpe("10.2.9.1_java")

    ClaraBase base = Mock(ClaraBase)
    CallbackWrapper wrapper = Mock(CallbackWrapper)
    EngineCallback callback = Stub(EngineCallback)

    Map<String, Subscription> subscriptions = [:]

    def "Start subscription uses front-end"() {
        when:
        makeSubscription("data:10.2.9.96_java:master:Simple").start(callback)

        then:
        1 * base.listen(frontEnd, _, _)
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
                base, subscriptions, frontEnd, Topic.wrap(topic)
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
