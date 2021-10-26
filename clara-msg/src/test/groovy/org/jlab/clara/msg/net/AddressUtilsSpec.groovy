/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.net

import spock.lang.Rollup
import spock.lang.Specification

@Rollup
class AddressUtilsSpec extends Specification {

    def "Check valid IPs"() {
        expect:
        AddressUtils.isIP(ip)

        where:
        ip << [
            "1.1.1.1",
            "255.255.255.255",
            "192.168.1.1",
            "10.10.1.1",
            "132.254.111.10",
            "26.10.2.10",
            "127.0.0.1",
        ]
    }

    def "Check invalid IPs"() {
        expect:
        !AddressUtils.isIP(ip)

        where:
        ip << [
            "10.10.10",
            "10.10",
            "10",
            "a.a.a.a",
            "10.10.10.a",
            "10.10.10.256",
            "222.222.2.999",
            "999.10.10.20",
            "2222.22.22.22",
            "22.2222.22.2",
        ]
    }

    def "Check IP only supports IPv4"() {
        expect:
        !AddressUtils.isIP(ip)

        where:
        ip << [
            "2001:cdba:0000:0000:0000:0000:3257:9652",
            "2001:cdba:0:0:0:0:3257:9652",
            "2001:cdba::3257:9652",
        ]
    }
}
