/*
 * Copyright (c) 2016.  Jefferson Lab (JLab). All rights reserved.
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
