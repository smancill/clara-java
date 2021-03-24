/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli

import org.jlab.clara.tests.Integration
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class ConfigParsersSpec extends Specification {

    def "Parser 'toString' parses non-empty string argument"() {
        expect:
        ConfigParsers.toString(*args) == expected

        where:
        args            || expected
        ["test_string"] || "test_string"
        ["test string"] || "test string"
    }

    def "Parser 'toString' throws on invalid or missing args"() {
        when:
        ConfigParsers.toString(*args)

        then:
        thrown IllegalArgumentException

        where:
        _ | args
        _ | [""]
        _ | []
    }

    def "Parser 'toStringOrEmpty' parses any string argument"() {
        expect:
        ConfigParsers.toStringOrEmpty(*args) == expected

        where:
        args            || expected
        ["test_string"] || "test_string"
        ["test string"] || "test string"
        [""]            || ""
        []              || ""
    }

    def "Parser 'toAlphaNum' parses a single alphanumeric word"() {
        expect:
        ConfigParsers.toAlphaNum(*args) == expected

        where:
        args        || expected
        ["stage01"] || "stage01"
        ["xxyy"]    || "xxyy"
        ["11111"]   || "11111"
    }

    def "Parser 'toAlphaNum' throws on invalid or missing arg"() {
        when:
        ConfigParsers.toAlphaNum(*args)

        then:
        thrown IllegalArgumentException

        where:
        _ | args
        _ | ["with space"]
        _ | ["with-sep"]
        _ | ["with_sep"]
        _ | ["a.string"]
        _ | [""]
        _ | []
    }

    def "Parser 'toAlphaNumOrEmpty' parses a single alphanumeric word"() {
        expect:
        ConfigParsers.toAlphaNumOrEmpty(*args) == expected

        where:
        args        || expected
        ["stage01"] || "stage01"
        ["xxyy"]    || "xxyy"
        ["11111"]   || "11111"
        [""]        || ""
        []          || ""
    }

    def "Parser 'toAlphaNumOrEmpty' throws on invalid arg"() {
        when:
        ConfigParsers.toAlphaNum(*args)

        then:
        thrown IllegalArgumentException

        where:
        _ | args
        _ | ["with space"]
        _ | ["with-sep"]
        _ | ["with_sep"]
        _ | ["a.string"]
    }

    def "Parser 'toNonWhitespace' parses any string without blank characters"() {
        expect:
        ConfigParsers.toNonWhitespace(*args) == expected

        where:
        args            || expected
        ["version-0.4"] || "version-0.4"
        ["mix05"]       || "mix05"
        ["xxyy"]        || "xxyy"
        ["11111"]       || "11111"
        ["with-sep"]    || "with-sep"
        ["with_sep"]    || "with_sep"
        ["a.string"]    || "a.string"
    }

    def "Parser 'toNonWhitespace' throws on invalid or missing arg"() {
        when:
        ConfigParsers.toNonWhitespace(*args)

        then:
        thrown IllegalArgumentException

        where:
        _ | args
        _ | ["with space"]
        _ | ["with\ttab"]
        _ | [""]
        _ | []
    }

    @Integration
    def "Parser 'toFile' parses a file path"() {
        expect:
        ConfigParsers.toFile(*args) == Path.of(expected)

        where:
        args                    || expected
        ["/bin/ls"]             || "/bin/ls"
        ["non-existing.txt"]    || "non-existing.txt"
        ["path/to/no-file.doc"] || "path/to/no-file.doc"
    }

    @Integration
    def "Parser 'toFile' throws on invalid or missing arg"() {
        when:
        ConfigParsers.toFile(*args)

        then:
        thrown IllegalArgumentException

        where:
        _ | args
        _ | ["/usr/bin"]
        _ | [""]
        _ | []
    }

    @Integration
    def "Parser 'toExistingFile' parses an existing file path"() {
        given:
        var tmp = Files.createTempFile("tmp", ".txt")

        expect:
        ConfigParsers.toExistingFile(tmp.toString()) == tmp

        cleanup:
        Files.delete(tmp)
    }

    @Integration
    def "Parser 'toExistingFile' throws on invalid or missing arg"() {
        when:
        ConfigParsers.toExistingFile(*args)

        then:
        thrown IllegalArgumentException

        where:
        _ | args
        _ | ["non-existing.file"]
        _ | ["/usr/bin"]
        _ | [""]
        _ | []
    }

    @Integration
    def "Parser 'toDirectory' parses a directory path"() {
        expect:
        ConfigParsers.toDirectory(*args) == Path.of(expected)

        where:
        args              || expected
        ["/tmp/"]         || "/tmp"
        ["non/existing/"] || "non/existing"
    }

    @Integration
    def "Parser 'toDirectory' throws on invalid or missing arg"() {
        when:
        ConfigParsers.toDirectory(*args)

        then:
        thrown IllegalArgumentException

        where:
        _ | args
        _ | ["/bin/ls"]
        _ | [""]
        _ | []
    }

    @Integration
    def "Parser 'toExistingDirectory' parses an existing directory path"() {
        expect:
        ConfigParsers.toExistingDirectory("/tmp") == Path.of("/tmp")
    }

    @Integration
    def "Parser 'toExistingDirectory' throws on invalid or missing arg"() {
        when:
        ConfigParsers.toExistingDirectory(*args)

        then:
        thrown IllegalArgumentException

        where:
        _ | args
        _ | ["non/existing/"]
        _ | ["/bin/ls"]
        _ | [""]
        _ | []
    }
}
