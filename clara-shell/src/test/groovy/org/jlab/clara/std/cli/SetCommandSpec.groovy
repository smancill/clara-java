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
        command.execute("servicesFile", userFile)

        then:
        config.getString(Config.SERVICES_FILE) == userFile
    }

    def "Set file list"() {
        given:
        var userFile = createTempFile("fileList")

        when:
        command.execute("fileList", userFile)

        then:
        config.getString(Config.FILES_LIST) == userFile
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
        command.execute("inputDir", userDir)

        then:
        config.getString(Config.INPUT_DIR) == userDir
    }

    def "Set output directory"() {
        given:
        var userDir = createTempDir("output")

        when:
        command.execute("outputDir", userDir)

        then:
        config.getString(Config.OUTPUT_DIR) == userDir
    }

    private static String createTempDir(String prefix) {
        Path tmpDir = Files.createTempDirectory(prefix)
        tmpDir.toFile().deleteOnExit()
        return tmpDir.toString()
    }

    private static String createTempFile(String prefix) {
        var tmpFile = File.createTempFile(prefix, "")
        tmpFile.deleteOnExit()
        return tmpFile.toString()
    }
}
