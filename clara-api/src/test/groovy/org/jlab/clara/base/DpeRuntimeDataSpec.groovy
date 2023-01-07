/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base

import org.jlab.clara.tests.Integration
import org.json.JSONArray
import org.json.JSONObject
import spock.lang.Shared
import spock.lang.Specification

@Integration
class DpeRuntimeDataSpec extends Specification {

    @Shared
    JSONObject json = JsonDataUtil.parseRuntimeExample()

    def "Parse DPE runtime data with registered containers"() {
        given:
        var data = new DpeRuntimeData(json)

        expect:
        with(data) {
            name().canonicalName() == "10.1.1.10_java"
            snapshotTime() != null
            cpuUsage() > 0.0
            memoryUsage() > 0L
            systemLoad() > 0.0
        }

        and:
        var containers = data.containers().collect { it.name().canonicalName() } as Set

        containers == [
            "10.1.1.10_java:trevor",
            "10.1.1.10_java:franklin",
            "10.1.1.10_java:michael",
        ] as Set
    }

    def "Parse DPE runtime with empty containers"() {
        given:
        var json = new JSONObject(json.toMap() << [containers: new JSONArray()])
        var data = new DpeRuntimeData(json)

        expect:
        data.containers().empty
    }
}
