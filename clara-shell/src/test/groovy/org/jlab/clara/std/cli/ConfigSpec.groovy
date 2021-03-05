/*
 * Copyright (c) 2017.  Jefferson Lab (JLab). All rights reserved.
 *
 * Permission to use, copy, modify, and distribute  this software and its
 * documentation for educational, research, and not-for-profit purposes,
 * without fee and without a signed licensing agreement.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING
 * OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY,
 * PROVIDED HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * This software was developed under the United States Government license.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
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
        variables.each {
            addVariable(config, it.key, it.value)
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
