/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys.pubsub

import spock.lang.Specification

class IdentityGeneratorSpec extends Specification {

    List<String> testIds = 10.collect { IdentityGenerator.getCtrlId() }

    def "Control ids should have 9 digits"() {
        expect:
        testIds.every { it.length() == 9 }
    }

    def "Control ids should have the same language identifier as first digit"() {
        expect:
        testIds.every { it[0] == '1' }
    }

    def "Control ids should have the same 3-digit node prefix after the language identifier"() {
        expect:
        var prefixes = testIds.collect { it.substring(1, 4) }
        prefixes.unique(false) == [prefixes[0]]
    }

    def "Control ids should have a unique random 5-digit suffix after the node prefix"() {
        expect:
        var suffixes = testIds.collect { it.substring(4) }
        suffixes.unique(false) == suffixes
    }
}
