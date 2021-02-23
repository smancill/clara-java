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

package org.jlab.clara.sys.ccc

import org.jlab.clara.base.core.ClaraConstants
import org.jlab.clara.base.error.ClaraException
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class CompositionCompilerSpec extends Specification {

    @Shared
    ServiceState inState = new ServiceState(ClaraConstants.UNDEFINED, ClaraConstants.UNDEFINED)

    @Subject
    CompositionCompiler compiler

    def "Compiling a composition that contains an invalid service name fails"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S1")

        and: "an invalid composition"
        var composition = "10.10.10.1_java:C:S1+10.10.10.1:C:S2+10.10.10.1:C:S4;"

        when:
        compiler.compile(composition)

        then:
        thrown ClaraException
    }

    def "Compiling a composition when the target service is missing in the composition fails"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S6")

        and: "a composition without the target service"
        var composition = "10.10.10.1_java:C:S1+10.10.10.1_java:C:S2+10.10.10.1_java:C:S3;"

        when:
        compiler.compile(composition)

        then:
        thrown ClaraException
    }

    def "Compiling a composition when the target service is at the beginning"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S1")

        and:
        var composition = "10.10.10.1_java:C:S1+10.10.10.1_java:C:S2+" +
                          "10.10.10.1_java:C:S3+10.10.10.1_java:C:S4;"

        when:
        compiler.compile(composition)

        then: "the target service output is the second service in the chain"
        compiler.unconditionalLinks == ["10.10.10.1_java:C:S2"] as Set
    }

    def "Compiling a composition when the target service is at the middle"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S2")

        and:
        var composition = "10.10.10.1_java:C:S1+10.10.10.1_java:C:S2+" +
                          "10.10.10.1_java:C:S3+10.10.10.1_java:C:S4;"

        when:
        compiler.compile(composition)

        then: "the target service output is the next service in the chain"
        compiler.unconditionalLinks == ["10.10.10.1_java:C:S3"] as Set
    }

    def "Compiling a composition when the target service is at the end"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S4")

        and:
        var composition = "10.10.10.1_java:C:S1+10.10.10.1_java:C:S2+" +
                          "10.10.10.1_java:C:S3+10.10.10.1_java:C:S4;"

        when:
        compiler.compile(composition)

        then: "the target service has not outputs"
        compiler.unconditionalLinks.empty
    }

    def "Compiling a composition with a logical OR branching"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S2")

        and:
        var composition = "10.10.10.1_java:C:S1+" +
                          "10.10.10.1_java:C:S2+" +
                          "10.10.10.1_java:C:S3,10.10.10.1_java:C:S4;"
        when:
        compiler.compile(composition)

        then: "obtain all outputs of the target service"
        compiler.unconditionalLinks == ["10.10.10.1_java:C:S3", "10.10.10.1_java:C:S4"] as Set
    }

    def "Compiling a composition with a multi-statement branching"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S2")

        and:
        var composition = "10.10.10.1_java:C:S1 + 10.10.10.1_java:C:S2 + 10.10.10.1_java:C:S3;" +
                          "10.10.10.1_java:C:S2 + 10.10.10.1_java:C:S4;"

        when:
        compiler.compile(composition)

        then: "obtain all outputs of the target service"
        compiler.unconditionalLinks == ["10.10.10.1_java:C:S3", "10.10.10.1_java:C:S4"] as Set
    }

    def "Compiling a composition with last service looping to the first"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S3")

        and:
        var composition = "10.10.10.1_java:C:S1 + 10.10.10.1_java:C:S3 + 10.10.10.1_java:C:S1;"

        when:
        compiler.compile(composition)

        then: "the target service output is the first service in the chain"
        compiler.unconditionalLinks == ["10.10.10.1_java:C:S1"] as Set
    }

    def "Compiling a composition with a single service and custom port"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1%9999_java:C:S1")

        and:
        var composition = "10.10.10.1%9999_java:C:S1;"

        when:
        compiler.compile(composition)

        then:
        compiler.unconditionalLinks.empty
    }

    def "Compiling a composition with multiple services and custom ports"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1%9999_java:C:S2")

        and:
        var composition = "10.10.10.1%10099_java:C:S1+" +
                          "10.10.10.1%9999_java:C:S2+" +
                          "10.10.10.1%10099_java:C:S3;"

        when:
        compiler.compile(composition)

        then:
        compiler.unconditionalLinks == ["10.10.10.1%10099_java:C:S3"] as Set
    }

    def "Using same compiler to compile a new different composition"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S3")

        when: "compile a composition"
        compiler.compile("10.10.10.1_java:C:S3+10.10.10.1_java:C:S2+10.10.10.1_java:C:S1;")

        and: "compile a second composition"
        compiler.compile("10.10.10.1_java:C:S3+10.10.10.1_java:C:S4+10.10.10.1_java:C:S5;")

        then: "the target service output is the one defined in the second composition"
        compiler.unconditionalLinks == ["10.10.10.1_java:C:S4"] as Set
    }

    def "Compiling a composition with an if-condition"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S1")

        and:
        var composition = """\
            10.10.10.1_java:C:S1;
            if (10.10.10.1_java:C:S1 == "FOO") {
              10.10.10.1_java:C:S1+10.10.10.1_java:C:S2;
            }"""
            .stripIndent()

        and:
        var ownState = new ServiceState("10.10.10.1_java:C:S1", "FOO")

        when:
        compiler.compile(composition)

        then: "the target service output is the service linked inside the if-condition"
        compiler.getLinks(ownState, inState) == ["10.10.10.1_java:C:S2"] as Set
    }

    def "Compiling a composition with an elif-condition"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S1")

        and: "a composition with an elseif-condition"
        var composition = """\
            10.10.10.1_java:C:S1;
            if (10.10.10.1_java:C:S1 == "FOO") {
              10.10.10.1_java:C:S1+10.10.10.1_java:C:S2;
            } elseif (10.10.10.1_java:C:S1 == "BAR") {
              10.10.10.1_java:C:S1+10.10.10.1_java:C:S3;
            } elseif (10.10.10.1_java:C:S1 == "FROZ") {
              10.10.10.1_java:C:S1+10.10.10.1_java:C:S4;
            }"""
            .stripIndent()

        and: "the state that makes the elseif-condition pass"
        var ownState = new ServiceState("10.10.10.1_java:C:S1", "FROZ")

        when:
        compiler.compile(composition)

        then: "the target service output is the service linked inside the elseif-condition"
        compiler.getLinks(ownState, inState) == ["10.10.10.1_java:C:S4"] as Set
    }

    def "Compiling a composition with an else-condition"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S1")

        and: "a composition with an else-condition"
        var composition = """\
            10.10.10.1_java:C:S1;
            if (10.10.10.1_java:C:S1 == "FOO") {
              10.10.10.1_java:C:S1+10.10.10.1_java:C:S2;
            } elseif (10.10.10.1_java:C:S1 == "BAR") {
              10.10.10.1_java:C:S1+10.10.10.1_java:C:S3;
            } elseif (10.10.10.1_java:C:S1 == "FROZ") {
              10.10.10.1_java:C:S1+10.10.10.1_java:C:S4;
            } else {
              10.10.10.1_java:C:S1+10.10.10.1_java:C:S5;
            }"""
            .stripIndent()

        and: "the state that makes the else-condition pass"
        var ownState = new ServiceState("10.10.10.1_java:C:S1", ClaraConstants.UNDEFINED)

        when:
        compiler.compile(composition)

        then: "the target service output is the service linked inside the else-condition"
        compiler.getLinks(ownState, inState) == ["10.10.10.1_java:C:S5"] as Set
    }

    def "Compiling a composition with a multi-statement branching inside a condition"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S1")

        and:
        var composition = """\
            10.10.10.1_java:C:S1;
            if (10.10.10.1_java:C:S1 == "FOO") {
              10.10.10.1_java:C:S1+10.10.10.1_java:C:S2;
              10.10.10.1_java:C:S1+10.10.10.1_java:C:S7;
            }"""
            .stripIndent()

        and: "the state that makes the condition pass"
        var ownState = new ServiceState("10.10.10.1_java:C:S1", "FOO")

        when:
        compiler.compile(composition)

        then: "the target service outputs are the services linked inside the condition"
        compiler.getLinks(ownState, inState) == ["10.10.10.1_java:C:S2", "10.10.10.1_java:C:S7"] as Set // codenarc-disable LineLength
    }

    def "Get conditional outputs for last service in an unconditional loop"() {
        given:
        compiler = new CompositionCompiler("10.10.10.1_java:C:S3")

        and: "a chain of services linked unconditionally in a loop"
        var composition = "10.10.10.1_java:C:S1 + 10.10.10.1_java:C:S3 + 10.10.10.1_java:C:S1;"

        and: "an undefined state (to get unconditionally linked services)"
        var ownState = new ServiceState("10.10.10.1_java:C:S3", ClaraConstants.UNDEFINED)

        when:
        compiler.compile(composition)

        then: "the target service output is the unconditionally linked service"
        compiler.getLinks(ownState, inState) == ["10.10.10.1_java:C:S1"] as Set
    }
}
