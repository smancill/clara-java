/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys.ccc

import spock.lang.Specification
import spock.lang.Subject

class SimpleCompilerSpec extends Specification {

    @Subject
    SimpleCompiler compiler

    def "Compiling a chain that contains an invalid service name fails"() {
        given:
        compiler = new SimpleCompiler("10.10.10.1_java:C:S1")

        when:
        compiler.compile("10.10.10.1_java:C:S1+10.10.10.1:C:S2+10.10.10.1:C:S4")

        then:
        thrown IllegalArgumentException
    }

    def "Compiling a chain when the target service is missing in the chain fails"() {
        given:
        compiler = new SimpleCompiler("10.10.10.1_java:C:S6")

        and:
        var composition = "10.10.10.1_java:C:S1+10.10.10.1_java:C:S2+10.10.10.1_java:C:S3"

        when:
        compiler.compile(composition)

        then:
        thrown IllegalArgumentException
    }

    def "Compiling a chain when the target service is at the beginning"() {
        given:
        compiler = new SimpleCompiler("10.10.10.1_java:C:S1")

        and:
        var composition = "10.10.10.1_java:C:S1+10.10.10.1_java:C:S2+" +
                          "10.10.10.1_java:C:S3+10.10.10.1_java:C:S4"

        when:
        compiler.compile(composition)

        then: "the target service output is the second service in the chain"
        compiler.outputs == ["10.10.10.1_java:C:S2"] as Set
    }

    def "Compiling a chain when the target service is at the middle"() {
        given:
        compiler = new SimpleCompiler("10.10.10.1_java:C:S2")

        and:
        var composition = "10.10.10.1_java:C:S1+10.10.10.1_java:C:S2+" +
                          "10.10.10.1_java:C:S3+10.10.10.1_java:C:S4"

        when:
        compiler.compile(composition)

        then: "the target service output is the next service in the chain"
        compiler.outputs == ["10.10.10.1_java:C:S3"] as Set
    }

    def "Compiling a chain when the target service is at the end"() {
        given:
        compiler = new SimpleCompiler("10.10.10.1_java:C:S4")

        and:
        var composition = "10.10.10.1_java:C:S1+10.10.10.1_java:C:S2+" +
                          "10.10.10.1_java:C:S3+10.10.10.1_java:C:S4"

        when:
        compiler.compile(composition)

        then: "the target service has no outputs"
        compiler.outputs.empty
    }

    def "Using same compiler to compile a new different chain"() {
        given:
        compiler = new SimpleCompiler("10.10.10.1_java:C:S3")

        when: "compile a chain"
        compiler.compile("10.10.10.1_java:C:S3+10.10.10.1_java:C:S2+10.10.10.1_java:C:S1")

        and: "compile a second chain"
        compiler.compile("10.10.10.1_java:C:S3+10.10.10.1_java:C:S4+10.10.10.1_java:C:S5")

        then: "the target service output is the one defined in the second chain"
        compiler.outputs == ["10.10.10.1_java:C:S4"] as Set
    }
}
