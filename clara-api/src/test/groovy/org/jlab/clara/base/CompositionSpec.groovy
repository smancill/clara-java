/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base

import spock.lang.Specification

class CompositionSpec extends Specification {

    def "Return the first service in a simple composition (chain)"() {
        given:
        var composition = new Composition("10.1.1.1:cont:S1+10.1.1.2:cont:S2")

        expect:
        composition.firstService() == "10.1.1.1:cont:S1"
    }

    def "Return the same string in a simple composition (chain)"() {
        given:
        var composition = new Composition("10.1.1.1:cont:S1+10.1.1.2:cont:S2")

        expect:
        composition.toString() == "10.1.1.1:cont:S1+10.1.1.2:cont:S2"
    }
}
