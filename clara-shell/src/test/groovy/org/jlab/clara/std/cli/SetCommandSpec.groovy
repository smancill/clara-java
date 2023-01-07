/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli

import org.jlab.clara.util.EnvUtils
import org.jline.terminal.Terminal
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

import java.nio.file.Files
import java.nio.file.Path

class SetCommandSpec extends Specification {

    Config config = new Config()

    @Subject
    SetCommand command = new SetCommand(new Context(Stub(Terminal), config))

    def "Use default session"() {
        expect:
        config.getString(Config.SESSION) == EnvUtils.userName()
    }

    def "Set session"() {
        when:
        command.execute("session", "trevor")

        then:
        config.getString(Config.SESSION) == "trevor"
    }

    @Tag("integration")
    def "Set services file"() {
        given:
        var userFile = createTempFile("yaml")

        when:
        command.execute("servicesFile", userFile.toString())

        then:
        config.getPath(Config.SERVICES_FILE) == userFile
    }

    @Tag("integration")
    def "Set file list"() {
        given:
        var userFile = createTempFile("fileList")

        when:
        command.execute("fileList", userFile.toString())

        then:
        config.getPath(Config.FILES_LIST) == userFile
    }

    def "Set max threads"() {
        when:
        command.execute("threads", "5")

        then:
        config.getInt(Config.MAX_THREADS) == 5
    }

    @Tag("integration")
    def "Set input directory"() {
        given:
        var userDir = createTempDir("input")

        when:
        command.execute("inputDir", userDir.toString())

        then:
        config.getPath(Config.INPUT_DIR) == userDir
    }

    @Tag("integration")
    def "Set output directory"() {
        given:
        var userDir = createTempDir("output")

        when:
        command.execute("outputDir", userDir.toString())

        then:
        config.getPath(Config.OUTPUT_DIR) == userDir
    }

    private static Path createTempDir(String prefix) {
        Files.createTempDirectory(prefix).tap {
            toFile().deleteOnExit()
        }
    }

    private static Path createTempFile(String prefix) {
        Files.createTempFile(prefix, "").tap {
            toFile().deleteOnExit()
        }
    }
}
