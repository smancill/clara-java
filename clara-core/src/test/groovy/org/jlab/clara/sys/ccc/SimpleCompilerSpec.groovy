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
