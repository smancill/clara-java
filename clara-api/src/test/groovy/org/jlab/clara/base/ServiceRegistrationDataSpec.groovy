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
class ServiceRegistrationDataSpec extends Specification {

    @Shared
    JSONObject json = JsonDataUtil.parseRegistrationExample()

    def "Parse service registration data"() {
        given:
        var data = new ServiceRegistrationData(JsonUtils.getService(json, 1, 0))

        expect:
        with(data) {
            name().canonicalName() == "10.1.1.10_java:franklin:Engine2"
            className() == "org.jlab.clara.examples.Engine2"
            startTime() != null
            poolSize() == 2
            author() == "Trevor"
            version() == "1.0"
            description() == "Some description of what it does."
        }
    }
}
