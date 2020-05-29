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
import org.jlab.clara.msg.sys.ConnectionFactory
import org.jlab.clara.msg.sys.pubsub.ProxyDriver
import org.jlab.clara.msg.sys.regdis.RegDriver
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class ConnectionManagerSpec extends Specification {

    @Subject ConnectionManager manager

    def setup() {
        var factory = Stub(ConnectionFactory) {
            createPublisherConnection(_, _) >> { ProxyAddress addr, _ ->
                var d = Stub(ProxyDriver)
                d.address >> addr
                return d
            }
            createRegistrarConnection(_) >> { RegAddress addr ->
                var d = Stub(RegDriver)
                d.address >> addr
                return d
            }
        }

        manager = new ConnectionManager(factory)
    }

    @Unroll("Create new #connectionType connections as needed")
    def "Create new connections as needed"() {
        given:
        var a1 = newAddress("10.2.9.1")
        var a2 = newAddress("10.2.9.2")

        when: "requested to get connections to multiple and/or repeated addresses"
        var addr = [a1, a2, a2]
        var conn = addr.collect { a -> getConnection(manager, a) }

        then: "the connections are created to the requested addresses"
        conn.address == addr

        and: "all connections are new, different instances"
        conn[0] !== conn[1]
        conn[0] !== conn[2]
        conn[1] !== conn[2]

        where:
        connectionType  | newAddress            | getConnection
        "proxy"         | ProxyAddress::new     | ConnectionManager::getProxyConnection
        "registrar"     | RegAddress::new       | ConnectionManager::getRegistrarConnection
    }

    @Unroll("Reuse #connectionType connections when available")
    def "Reuse connections when available"() {
        given: "existing connections to multiple and/or repeated addresses"
        var a1 = newAddress("10.2.9.1")
        var a2 = newAddress("10.2.9.2")

        var addr = [a1, a2, a2]
        var cc1 = addr.collect { a -> getConnection(manager, a) }

        when: "releasing the connections back to the pool"
        cc1.each { c -> releaseConnection(manager, c) }

        and: "requesting new connections to the same addresses"
        var cc2 = addr.collect { a -> getConnection(manager, a) }

        then: "the released connections are reused instead of creating new instances"
        [cc1, cc2].transpose().every { c1, c2 -> c1 === c2 }

        where:
        connectionType  | newAddress
        "proxy"         | ProxyAddress::new
        "registrar"     | RegAddress::new
        ___
        getConnection                               | releaseConnection
        ConnectionManager::getProxyConnection       | ConnectionManager::releaseProxyConnection
        ConnectionManager::getRegistrarConnection   | ConnectionManager::releaseRegistrarConnection
    }
}
