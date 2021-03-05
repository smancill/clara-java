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
