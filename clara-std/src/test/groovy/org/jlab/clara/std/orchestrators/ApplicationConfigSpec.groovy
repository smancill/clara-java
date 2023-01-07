/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators

import org.jlab.clara.base.ServiceName
import org.json.JSONObject
import spock.lang.Specification

class ApplicationConfigSpec extends Specification {

    private static final GLOBAL = new JSONObject(
        limit: 9000,
        geometry: [run: 10, variation: "custom"],
        log: false,
    )

    private static final R = new JSONObject(max_events: 100)
    private static final W = new JSONObject(compression: 2)

    private static final S1 = new JSONObject(layers: ["inner", "outer"], filter: "skip", log: true)
    private static final S2 = new JSONObject(hits: 10, filter: "greedy")

    private static final SN1 = new ServiceName("10.1.1.1_java:master:S1")
    private static final SN2 = new ServiceName("10.1.1.1_java:master:S2")

    def "Without an IO config section the IO service config is empty"() {
        given:
        var data = new JSONObject()

        when:
        var config = new ApplicationConfig(data)

        then:
        config.reader().length() == 0
        config.writer().length() == 0
    }

    def "Parse the IO service config data from the IO config section"() {
        given:
        var data = new JSONObject("io-services": [reader: R, writer: W])

        when:
        var config = new ApplicationConfig(data)

        then:
        config.reader().similar(R)
        config.writer().similar(W)
    }

    def "Without a service config section any service config is empty"() {
        given:
        var data = new JSONObject()

        when:
        var config = new ApplicationConfig(data)

        then:
        config.get(SN1).length() == 0
        config.get(SN2).length() == 0
    }

    def "Services not defined in the service config section have empty config data"() {
        given:
        var data = new JSONObject(services: [S1: S1])

        when:
        var config = new ApplicationConfig(data)

        then:
        config.get(SN2).length() == 0
    }

    def "Parse the service config data when defined in the service config section"() {
        given:
        var data = new JSONObject(services: [S1: S1, S2: S2])

        when:
        var config = new ApplicationConfig(data)

        then:
        config.get(SN1).similar(S1)
        config.get(SN2).similar(S2)
    }

    def "The global config data is used for all services"() {
        given:
        var data = new JSONObject(global: GLOBAL)

        when:
        var config = new ApplicationConfig(data)

        then:
        config.get(SN1).similar(GLOBAL)
        config.get(SN2).similar(GLOBAL)
    }

    def "With global and service config sections the service config is the merged result"() {
        given:
        var data = new JSONObject(global: GLOBAL, services: [S1: S1, S2: S2])

        when:
        var config = new ApplicationConfig(data)

        then:
        var expected = new JSONObject(
            limit: 9000,
            geometry: [run: 10, variation: "custom"],
            log: false,
            hits: 10,
            filter: "greedy",
        )

        config.get(SN2).similar(expected)
    }

    def "With global and service config section the service config values override the global"() {
        given:
        var data = new JSONObject(global: GLOBAL, services: [S1: S1, S2: S2])

        when:
        var config = new ApplicationConfig(data)

        then:
        config.get(SN1).getBoolean("log")
    }

    def "Expand variables on template string values"() {
        given:
        var test = [
            variation: "custom",
            run: 10,
            config_file: '${input_file?replace(".dat", ".conf")}',
            user: '${"john doe"?capitalize}',
        ]
        var data = new JSONObject(services: [Test: test])

        var model = [input_file: "run10_20180101.dat"]

        when:
        var config = new ApplicationConfig(data, model)

        then:
        var serviceData = config.get(new ServiceName("10.1.1.1_java:master:Test"))
        with(serviceData) {
            getString("config_file") == "run10_20180101.conf"
            getString("user") == "John Doe"
        }
    }
}
