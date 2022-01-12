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

    def "Set services file"() {
        given:
        var userFile = createTempFile("yaml")

        when:
        command.execute("servicesFile", userFile.toString())

        then:
        config.getPath(Config.SERVICES_FILE) == userFile
    }

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

    def "Set input directory"() {
        given:
        var userDir = createTempDir("input")

        when:
        command.execute("inputDir", userDir.toString())

        then:
        config.getPath(Config.INPUT_DIR) == userDir
    }

    def "Set output directory"() {
        given:
        var userDir = createTempDir("output")

        when:
        command.execute("outputDir", userDir.toString())

        then:
        config.getPath(Config.OUTPUT_DIR) == userDir
    }

    private static Path createTempDir(String prefix) {
        Path tmpDir = Files.createTempDirectory(prefix)
        tmpDir.toFile().deleteOnExit()
        return tmpDir
    }

    private static Path createTempFile(String prefix) {
        Path tmpFile = Files.createTempFile(prefix, "")
        tmpFile.toFile().deleteOnExit()
        return tmpFile
    }
}
