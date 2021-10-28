/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
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
