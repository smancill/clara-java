/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
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
