/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.engine

import org.jlab.clara.engine.EngineSpecification.ParseException
import org.jlab.clara.tests.Integration
import spock.lang.Specification

@Integration
class EngineSpecificationSpec extends Specification {

    def "Constructor throws if specification file is not found"() {
        when:
        new EngineSpecification("std.services.convertors.EvioToNothing")

        then:
        var ex = thrown(ParseException)
        ex.message =~ "Service specification file not found"
    }

    def "Constructor throws if YAML specification is malformed"() {
        when:
        new EngineSpecification("resources/service-spec-bad-1")

        then:
        var ex = thrown(ParseException)
        ex.message =~ "Unexpected YAML content"
    }

    def "Parse service specification"() {
        given:
        var spec = new EngineSpecification("resources/service-spec-simple")

        expect:
        with(spec) {
            name() == "SomeService"
            engine() == "std.services.SomeService"
            type() == "java"
        }
    }

    def "Parse author specification"() {
        given:
        var spec = new EngineSpecification("resources/service-spec-simple")

        expect:
        with(spec) {
            author() == "Sebastian Mancilla"
            email() == "smancill@jlab.org"
        }
    }

    def "Parse version specification of #type value"() {
        given:
        var spec = new EngineSpecification(specFile)

        expect:
        spec.version() == version

        where:
        type      | specFile                        || version
        "integer" | "resources/service-spec-1"      || "0.8"
        "double"  | "resources/service-spec-simple" || "2"
    }

    def "Constructor throws if specification file has missing required key"() {
        when:
        new EngineSpecification("resources/service-spec-bad-2")

        then:
        var ex = thrown(ParseException)
        ex.message =~ "Missing key:"
    }

    def "Constructor throws if specification file has key of wrong type"() {
        when:
        new EngineSpecification("resources/service-spec-bad-3")

        then:
        var ex = thrown(ParseException)
        ex.message =~ "Bad type for:"
    }
}
