/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli

import spock.lang.Specification

class ConfigSpec extends Specification {

    def "Store any number of variables"() {
        given:
        var config = new Config()

        and:
        var variables = [
            vstring1: "string",
            vint1: 1,
            vint2: 50,
            vlong1: 10L,
            vdouble1: 100,
            vdouble2: 30.4,
            vbool: false,
        ]

        when:
        variables.each { name, value ->
            addVariable(config, name, value)
        }

        then:
        variables.keySet().every {
            config.hasVariable it
            config.hasValue it
        }
    }

    def "Retrieve the variable value using the right type"() {
        given:
        var config = new Config()

        when:
        addVariable(config, name, value)

        then:
        getter(config, name) == expected

        where:
        name      | value   | getter             || expected
        "vstring" | "value" | Config::getString  || "value"
        "vint"    | 10      | Config::getInt     || 10
        "vlong"   | 200L    | Config::getLong    || 200L
        "vint"    | 10      | Config::getLong    || 10L
        "vdouble" | 30.0    | Config::getDouble  || 30.0d
        "vdouble" | 50      | Config::getDouble  || 50.0d
        "vbool"   | true    | Config::getBoolean || true
    }

    def "Parse the variable value using the right type if initially stored as string"() {
        given:
        var config = new Config()

        when:
        addVariable(config, name, value)

        then:
        getter(config, name) == expected

        where:
        name      | value  | getter             || expected
        "vint"    | "5"    | Config::getInt     || 5
        "vlong"   | "500"  | Config::getLong    || 500L
        "vdouble" | "24.5" | Config::getDouble  || 24.5d
        "vbool"   | "true" | Config::getBoolean || true
        "vbool"   | "yes"  | Config::getBoolean || false
    }

    def "Throw when using the wrong type to retrieve the variable value"() {
        given:
        var config = new Config()

        and:
        addVariable(config, name, value)

        when:
        wrongGetter(config, name)

        then:
        var ex = thrown(IllegalArgumentException)
        ex.message =~ ""

        where:
        name      | value        | wrongGetter
        "vstring" | "value"      | Config::getInt
        "vstring" | "value"      | Config::getLong
        "vstring" | "value"      | Config::getDouble
        "vint"    | 10           | Config::getString
        "vint"    | 10           | Config::getBoolean
        "vobj"    | new Object() | Config::getInt
        "vobj"    | new Object() | Config::getDouble
        "vobj"    | new Object() | Config::getString
    }

    private static void addVariable(Config c, String name, Object value) {
        var variable = ConfigVariable.newBuilder(name, "").withInitialValue(value).build()
        c.addVariable(variable)
    }
}
