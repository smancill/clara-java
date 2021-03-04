/*
 * Copyright (c) 2018.  Jefferson Lab (JLab). All rights reserved.
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

import org.jline.reader.Candidate
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import org.jline.reader.impl.completer.StringsCompleter
import spock.lang.Specification

class ConfigVariableSpec extends Specification {

    def "Build with default parameters"() {
        given:
        var variable = ConfigVariable.newBuilder("var", "test variable").build()

        expect:
        with(variable) {
            name == "var"
            description == "test variable"
            !hasValue()
        }
    }

    def "Build with initial value"() {
        given:
        var variable = ConfigVariable.newBuilder("var", "test variable")
            .withInitialValue(value)
            .build()

        expect:
        with(variable) {
            hasValue()
            getValue() == value
        }

        where:
        _ | value
        _ | 12
        _ | 12.8
        _ | false
    }

    def "Build with default parser"() {
        given:
        var variable = ConfigVariable.newBuilder("var", "test variable").build()

        when:
        variable.parseValue("value")

        then:
        variable.value == "value"
    }

    def "Build with custom parser"() {
        given:
        var variable = ConfigVariable.newBuilder("var", "test variable")
            .withParser(ConfigParsers::toPositiveInteger)
            .build()

        when:
        variable.parseValue("187")

        then:
        variable.value == 187
    }

    def "Build with default completer"() {
        given:
        var variable = ConfigVariable.newBuilder("var", "test variable").build()

        when:
        var candidates = getCompletion(variable, Stub(LineReader), Stub(ParsedLine))

        then:
        candidates.empty
    }

    def "Build with expected string values"() {
        given:
        var variable = ConfigVariable.newBuilder("var", "a test variable")
            .withExpectedValues("hello", "hola", "bonjour")
            .build()

        when:
        var candidates = getCompletion(variable, Stub(LineReader), Stub(ParsedLine))

        then:
        candidates == ["hello", "hola", "bonjour"]
    }

    def "Build with expected object values"() {
        given:
        var variable = ConfigVariable.newBuilder("var", "a test variable")
            .withExpectedValues(4, 8, 15)
            .build()

        when:
        var candidates = getCompletion(variable, Stub(LineReader), Stub(ParsedLine))

        then:
        candidates == ["4", "8", "15"]
    }

    def "Build with custom completer"() {
        given:
        var variable = ConfigVariable.newBuilder("var", "test variable")
            .withCompleter(new StringsCompleter("one", "two"))
            .build()

        when:
        var candidates = getCompletion(variable, Stub(LineReader), Stub(ParsedLine))

        then:
        candidates == ["one", "two"]
    }

    private static List<String> getCompletion(variable, reader, line) {
        List<Candidate> candidates = []
        variable.completer.complete(reader, line, candidates)
        return candidates.collect { it.value() }
    }
}
