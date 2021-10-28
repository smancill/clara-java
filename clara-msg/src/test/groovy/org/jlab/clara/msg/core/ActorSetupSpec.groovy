/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core

import org.jlab.clara.msg.net.ProxyAddress
import org.jlab.clara.msg.net.RegAddress
import spock.lang.Specification

class ActorSetupSpec extends Specification {

    def "Configure an actor with default values"() {
        when:
        var setup = ActorSetup.newBuilder().build()

        then:
        with(setup) {
            proxyAddress() == new ProxyAddress()
            registrarAddress() == new RegAddress()
            poolSize() == DEFAULT_POOL_SIZE
            subscriptionMode() == CallbackMode.MULTI_THREAD
        }
    }

    def "Configure an actor with user-defined values"() {
        when:
        var setup = ActorSetup.newBuilder()
            .withProxy(new ProxyAddress("10.1.1.10"))
            .withRegistrar(new RegAddress("10.1.1.1"))
            .withPoolSize(5)
            .withSubscriptionMode(CallbackMode.SINGLE_THREAD)
            .build()

        then:
        with(setup) {
            proxyAddress() == new ProxyAddress("10.1.1.10")
            registrarAddress() == new RegAddress("10.1.1.1")
            poolSize() == 5
            subscriptionMode() == CallbackMode.SINGLE_THREAD
        }
    }
}
