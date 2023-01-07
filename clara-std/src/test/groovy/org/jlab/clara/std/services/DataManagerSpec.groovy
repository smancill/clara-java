/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.services

import org.jlab.clara.engine.EngineData
import org.jlab.clara.engine.EngineDataType
import org.jlab.clara.engine.EngineStatus
import org.jlab.clara.tests.Integration
import org.json.JSONObject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer

class DataManagerSpec extends Specification {

    @Shared String testFile = getClass().getResource("/collider.txt").getPath()

    @TempDir Path tmpDir

    @Subject DataManager dm = new DataManager()

    def "Set default paths"() {
        given: "setting the base directory for all paths"
        dm = new DataManager("/clara/")

        expect: "the default input/output paths have the given base directory as prefix"
        with(dm.configuration) {
            getString("input_path") == "/clara/data/input"
            getString("output_path") == "/clara/data/output"
            getString("stage_path") == "/scratch"
        }
    }

    def "Set all paths"() {
        given: "user-defined values for all paths"
        EngineData config = createJsonRequest(
            input_path: "/mnt/exp/in",
            output_path: "/mnt/exp/out",
            stage_path: "/tmp/files",
        )

        when:
        dm.configure(config)

        then:
        with(dm.configuration) {
            getString("input_path") == "/mnt/exp/in"
            getString("output_path") == "/mnt/exp/out"
            getString("stage_path") == "/tmp/files"
        }
    }

    def "Config input-output paths only"() {
        given: "user-defined values for input/output paths but not for stage"
        EngineData config = createJsonRequest(
            input_path: "/mnt/exp/in",
            output_path: "/mnt/exp/out",
        )

        when:
        dm.configure(config)

        then:
        with(dm.configuration) {
            getString("input_path") == "/mnt/exp/in"
            getString("output_path") == "/mnt/exp/out"
            getString("stage_path") == "/scratch"
        }
    }

    def "Config returns error on empty #path"() {
        given: "an empty #path"
        var paths = [input_path: "/in", output_path: "/out", stage_path: "/tmp"] << [(path): ""]

        when:
        EngineData result = dm.configure(createJsonRequest(paths))

        then:
        assertEngineError result, error

        where:
        path          || error
        "input_path"  || "empty input"
        "output_path" || "empty output"
        "stage_path"  || "empty stage"
    }

    @Integration
    def "Config returns error when #path exists and is not a directory"() {
        given: "#path incorrectly set to an existing file name"
        var tmp = makeFile("tmp.tmp")
        var paths = [input_path: "/in", output_path: "/out", stage_path: "/tmp"] << [(path): tmp]

        when:
        EngineData result = dm.configure(createJsonRequest(paths))

        then:
        assertEngineError result, "not a directory"

        where:
        _ | path
        _ | "input_path"
        _ | "output_path"
        _ | "stage_path"
    }

    def "Config returns error on missing #path"() {
        given: "a missing value for #path"
        var paths = [input_path: "/in", output_path: "/out", stage_path: "/tmp"].tap {
            remove(path)
        }

        when:
        EngineData result = dm.configure(createJsonRequest(paths))

        then:
        assertEngineError result, "invalid request"

        where:
        _ | path
        _ | "input_path"
        _ | "output_path"
    }

    def "Config returns error on request with wrong mime-type"() {
        given: "a request that is not JSON"
        EngineData config = new EngineData().tap { setData("text/string", "bad config") }

        when:
        EngineData result = dm.configure(config)

        then:
        assertEngineError result, "wrong mime-type: text/string"
    }

    @Integration
    def "Execute action 'stage_input' creates directory before staging input file"() {
        given: "a non-existing stage directory"
        TestPaths paths = configTestPaths { paths ->
            Files.delete(paths.stageDir)
        }

        and:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "stage_input",
            file: paths.inputFileName,
        )

        expect:
        Files.notExists paths.stageDir

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineSuccess result

        and:
        Files.exists paths.stageDir
    }

    @Integration
    def "Execute action 'stage_input' stages input file"() {
        given:
        TestPaths paths = configTestPaths { }

        and:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "stage_input",
            file: paths.inputFileName,
        )

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineSuccess result

        and:
        Files.exists paths.stagedInputFile
    }

    @Integration
    def "Execute action 'stage_input' stages input file into existing symlink directory"() {
        given:
        TestPaths paths = configTestPaths { paths ->
            paths.stageDir = makeSymlink(paths.stageDir)
        }

        and:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "stage_input",
            file: paths.inputFileName,
        )

        expect:
        Files.isSymbolicLink paths.stageDir

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineSuccess result

        and:
        Files.exists paths.stagedInputFile
    }

    @Integration
    def "Execute action 'remove_input' removes staged input file"() {
        given:
        TestPaths paths = configTestPaths { paths ->
            Files.copy(paths.inputFile, paths.stagedInputFile)
        }

        and:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "remove_input",
            file: paths.inputFileName,
        )

        expect:
        Files.exists paths.stagedInputFile

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineSuccess result

        and:
        Files.notExists paths.stagedInputFile
    }

    @Integration
    def "Execute action 'save_output' creates directory before saving output file"() {
        given:
        TestPaths paths = configTestPaths { paths ->
            Files.copy(paths.inputFile, paths.stagedOutputFile)
            Files.delete(paths.outputDir)
        }

        and:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "save_output",
            file: paths.inputFileName,
        )

        expect:
        Files.exists paths.stagedOutputFile
        Files.notExists paths.outputDir

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineSuccess result

        and:
        Files.exists paths.outputDir
    }

    @Integration
    def "Execute action 'save_output' saves output file"() {
        given:
        TestPaths paths = configTestPaths { paths ->
            Files.copy(paths.inputFile, paths.stagedOutputFile)
        }

        and:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "save_output",
            file: paths.inputFileName,
        )

        expect:
        Files.exists paths.stagedOutputFile
        Files.notExists paths.outputFile

        when:
        EngineData result = dm.execute(request)
        print result.description

        then:
        assertEngineSuccess result

        and:
        Files.notExists paths.stagedOutputFile
        Files.exists paths.outputFile
    }

    @Integration
    def "Execute action 'save_output' saves output file into existing symlink directory"() {
        given:
        TestPaths paths = configTestPaths { paths ->
            Files.copy(paths.inputFile, paths.stagedOutputFile)
            paths.outputDir = makeSymlink(paths.outputDir)
        }

        and:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "save_output",
            file: paths.inputFileName,
        )

        expect:
        Files.isSymbolicLink paths.outputDir

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineSuccess result

        and:
        Files.notExists paths.stagedOutputFile
        Files.exists paths.outputFile
    }

    @Integration
    def "Execute action 'clear_stage' removes stage directory"() {
        given:
        TestPaths paths = configTestPaths { paths ->
            Files.copy(paths.inputFile, paths.stagedInputFile)
        }

        and:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "clear_stage",
        )

        expect:
        Files.exists paths.stagedInputFile

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineSuccess result

        and:
        Files.notExists paths.stageDir
    }

    @Integration
    def "Execute action 'clear_stage' does not fail if stage directory is already removed"() {
        given:
        TestPaths paths = configTestPaths() { paths ->
            Files.delete(paths.stageDir)
        }

        and:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "clear_stage",
        )

        expect:
        Files.notExists paths.stageDir

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineSuccess result

        and:
        Files.notExists paths.stageDir
    }

    def "Execute action 'stage_input' returns error on IO error"() {
        given:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "stage_input",
            file: "missing.ev",
        )

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineError result, "could not complete request"
    }

    @Integration
    def "Execute action 'remove_input' returns error on IO error"() {
        given:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "remove_input",
            file: "file.ev",
        )

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineError result, "could not complete request"
    }


    @Integration
    def "Execute action 'save_output' returns error on IO error"() {
        given:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "save_output",
            file: "file.ev",
        )

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineError result, "could not complete request"
    }

    def "Execute returns error on missing property"() {
        given:
        EngineData request = createJsonRequest(
            command: "bad_action",
            file: "/mnt/exp/in/file.ev",
        )

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineError result, "invalid request"
    }

    def "Execute returns error on wrong action"() {
        given:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "bad_action",
            file: "file.ev",
        )

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineError result, "invalid action value: bad_action"
    }

    def "Execute returns error on missing mime-type"() {
        given:
        EngineData request = new EngineData().tap { setData("text/number", 42) }

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineError result, "wrong mimetype: text/number"
    }

    def "Execute returns error on input file with full path"() {
        given:
        EngineData request = createJsonRequest(
            type: "exec",
            action: "stage_input",
            file: "/mnt/exp/in/file.ev",
        )

        when:
        EngineData result = dm.execute(request)

        then:
        assertEngineError result, "invalid input file name"
    }

    private void assertEngineError(EngineData data, String errorMsg) {
        with(data) {
            data.status == EngineStatus.ERROR
            data.description =~ errorMsg
        }
    }

    private void assertEngineSuccess(EngineData data) {
        with(data) {
            data.status != EngineStatus.ERROR
        }
    }

    private static EngineData createJsonRequest(Map data) {
        var request = new JSONObject(data)
        new EngineData().tap {
            setData(EngineDataType.JSON.mimeType(), request.toString())
        }
    }

    private static class TestPaths {

        final Path inputFile

        Path outputDir
        Path stageDir

        TestPaths(Path inputPath, Path tmpDir) {
            inputFile = inputPath
            outputDir = tmpDir.resolve("output")
            stageDir = tmpDir.resolve("stage")
        }

        Path getInputDir() { inputFile.parent }

        String getInputFileName() { inputFile.fileName }

        Path getOutputFile() { outputDir.resolve("out_" + inputFileName) }

        Path getStagedInputFile() { stageDir.resolve(inputFile.fileName) }

        Path getStagedOutputFile() { stageDir.resolve(outputFile.fileName) }
    }

    private TestPaths configTestPaths(Consumer<TestPaths> preparePaths = { }) {
        var paths = new TestPaths(Path.of(testFile), tmpDir)

        Files.createDirectory(paths.outputDir)
        Files.createDirectory(paths.stageDir)
        preparePaths(paths)

        EngineData config = createJsonRequest(
            input_path: paths.inputDir.toString(),
            output_path: paths.outputDir.toString(),
            stage_path: paths.stageDir.toString(),
        )
        dm.configure(config)

        return paths
    }

    private Path makeSymlink(Path target) {
        Files.createSymbolicLink(tmpDir.resolve("link"), target)
    }

    private Path makeFile(String name) {
        Files.createFile(tmpDir.resolve(name))
    }
}
