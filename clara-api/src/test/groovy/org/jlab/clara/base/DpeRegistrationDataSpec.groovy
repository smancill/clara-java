/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base

import org.jlab.clara.tests.Integration
import org.json.JSONArray
import org.json.JSONObject
import spock.lang.Specification

@Integration
class DpeRegistrationDataSpec extends Specification {

    JSONObject json = JsonDataUtil.parseRegistrationExample()

    def "Parse DPE registration data with registered containers"() {
        given:
        var data = new DpeRegistrationData(json)

        expect:
        with(data) {
            name().canonicalName() == "10.1.1.10_java"
            claraHome() == "/home/user/clara"
            session() == "los_santos"
            startTime() != null
            numCores() == 8
            memorySize() > 0
        }

        and:
        var containers = data.containers().collect { it.name().canonicalName() } as Set

        containers == [
            "10.1.1.10_java:trevor",
            "10.1.1.10_java:franklin",
            "10.1.1.10_java:michael",
        ] as Set
    }

    def "Parse DPE registration with empty containers"() {
        given:
        var json = json.put("containers", new JSONArray())
        var data = new DpeRegistrationData(json)

        expect:
        data.containers().empty
    }
}
