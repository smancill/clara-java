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
class ServiceRuntimeDataSpec extends Specification {

    @Shared
    JSONObject json = JsonDataUtil.parseRuntimeExample()

    def "Parse service registration data"() {
        given:
        var data = new ServiceRuntimeData(JsonUtils.getService(json, 1, 0))

        expect:
        with(data) {
            name().canonicalName() == "10.1.1.10_java:franklin:Engine2"
            snapshotTime() != null
            numRequests() == 2000
            numFailures() == 200
            sharedMemoryReads() == 1800
            sharedMemoryWrites() == 1800
            bytesReceived() == 100
            bytesSent() == 330
            executionTime() == 243235243543
        }
    }
}
