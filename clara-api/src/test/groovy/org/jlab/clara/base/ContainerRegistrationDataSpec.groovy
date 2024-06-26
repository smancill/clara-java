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
class ContainerRegistrationDataSpec extends Specification {

    @Shared
    JSONObject json = JsonDataUtil.parseRegistrationExample()

    def "Parse container registration data with registered services"() {
        given:
        var data = new ContainerRegistrationData(JsonUtils.getContainer(json, 1))

        expect:
        with(data) {
            name().canonicalName() == "10.1.1.10_java:franklin"
            startTime() != null
        }

        and:
        var services = data.services().collect { it.name().canonicalName() } as Set

        services == ["10.1.1.10_java:franklin:Engine2", "10.1.1.10_java:franklin:Engine3"] as Set
    }

    def "Parse container registration with empty services"() {
        given:
        var data = new ContainerRegistrationData(JsonUtils.getContainer(json, 2))

        expect:
        data.services().empty
    }
}
