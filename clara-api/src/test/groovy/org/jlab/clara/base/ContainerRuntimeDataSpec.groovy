/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base

import org.jlab.clara.util.report.JsonUtils
import org.json.JSONObject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Tag

@Tag("integration")
class ContainerRuntimeDataSpec extends Specification {

    @Shared
    JSONObject json = JsonDataUtil.parseRuntimeExample()

    def "Parse container runtime data with registered services"() {
        given:
        var data = new ContainerRuntimeData(JsonUtils.getContainer(json, 1))

        expect:
        with(data) {
            name().canonicalName() == "10.1.1.10_java:franklin"
            snapshotTime() != null
            numRequests() == 3500
        }

        and:
        var services = data.services().collect { it.name().canonicalName() } as Set

        services == ["10.1.1.10_java:franklin:Engine2", "10.1.1.10_java:franklin:Engine3"] as Set
    }

    def "Parse container runtime data with empty services"() {
        given:
        var data = new ContainerRuntimeData(JsonUtils.getContainer(json, 2))

        expect:
        data.services().empty
    }
}
