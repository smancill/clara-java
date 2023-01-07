/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators

import org.jlab.clara.util.FileUtils
import spock.lang.Rollup
import spock.lang.Specification
import spock.lang.Subject
import spock.util.mop.Use

import java.nio.file.Path

@Rollup
class OrchestratorPathsSpec extends Specification {

    private static final INPUT_FILE = Path.of("input.ev")
    private static final OUTPUT_FILE = Path.of("output.ev")

    private static final VALID_FILES = [
        [arg: "file.ev", expectedDir: ""],
        [arg: "./file.ev", expectedDir: ""],
        [arg: "a/f.e", expectedDir: "a"],
        [arg: "a/b/f", expectedDir: "a/b"],
        [arg: "./d/a.bin", expectedDir: "d"],
        [arg: "./a/b/c/../d/r.data", expectedDir: "a/b/d"],
        [arg: "/m/n/o/file.ev", expectedDir: "/m/n/o"],
        [arg: "/m/./n/../f", expectedDir: "/m"],
    ]

    private static final VALID_DIRS = [
        [arg: "", expectedDir: ""],
        [arg: ".", expectedDir: ""],
        [arg: "a", expectedDir: "a"],
        [arg: "a/b", expectedDir: "a/b"],
        [arg: "./d", expectedDir: "d"],
        [arg: "./a/b/c/../d", expectedDir: "a/b/d"],
        [arg: "/m/n/o", expectedDir: "/m/n/o"],
        [arg: "/m/./n/..", expectedDir: "/m"],
    ]

    @Subject
    OrchestratorPaths paths

    @Use(OrchestratorPathsExtensions)
    def "Single file mode requires a single input file path"() {
        when:
        paths = new OrchestratorPaths.Builder(inputFile, OUTPUT_FILE).build()

        then:
        paths.inputNames == [inputFile.fileName.toString()]
        paths.numFiles() == 1

        where:
        [inputFile, _] << toPaths(VALID_FILES)
    }

    def "Single file mode sets an absolute normalized input dir based on input file path"() {
        when:
        paths = new OrchestratorPaths.Builder(inputFile, OUTPUT_FILE).build()

        then:
        paths.inputDir.absolute
        paths.inputDir == inputDir

        where:
        [inputFile, inputDir] << toPaths(VALID_FILES)
    }

    @Use(OrchestratorPathsExtensions)
    def "Single file mode requires a single output file path"() {
        when:
        paths = new OrchestratorPaths.Builder(INPUT_FILE, outputFile).build()

        then:
        paths.outputNames == [outputFile.fileName.toString()]

        where:
        [outputFile, _] << toPaths(VALID_FILES)
    }

    def "Single file mode sets an absolute normalized output dir based on output file path"() {
        when:
        paths = new OrchestratorPaths.Builder(INPUT_FILE, outputFile).build()

        then:
        paths.outputDir.absolute
        paths.outputDir == outputDir

        where:
        [outputFile, outputDir] << toPaths(VALID_FILES)
    }

    @Use(OrchestratorPathsExtensions)
    def "File list mode requires a list of input file names"() {
        when:
        paths = new OrchestratorPaths.Builder(["f1.ev", "f2.ev", "f3.ev"]).build()

        then:
        paths.inputNames == ["f1.ev", "f2.ev", "f3.ev"]
        paths.numFiles() == 3
    }

    @Use(OrchestratorPathsExtensions)
    def "File list mode generates the output files list from the input files list"() {
        when:
        paths = new OrchestratorPaths.Builder(["c1.evio", "c2.evio"]).build()

        then:
        paths.outputNames == ["out_c1.evio", "out_c2.evio"]
    }

    def "File list mode shall not receive full paths"() {
        when:
        paths = new OrchestratorPaths.Builder(["/a/b/c1.evio", "/a/b/c2.evio"]).build()

        then:
        var ex = thrown(OrchestratorConfigException)
        ex.message == "Input file cannot be a path: /a/b/c1.evio"
    }

    def "File list mode shall not receive relative paths"() {
        when:
        paths = new OrchestratorPaths.Builder(["c1.evio", "b/c2.evio"]).build()

        then:
        var ex = thrown(OrchestratorConfigException)
        ex.message == "Input file cannot be a path: b/c2.evio"
    }

    def "File list mode uses default data directories based on CLARA_HOME"() {
        when:
        paths = new OrchestratorPaths.Builder(["input.ev"]).build()

        then:
        with(paths) {
            inputDir == FileUtils.claraPath("data", "input")
            outputDir == FileUtils.claraPath("data", "output")
            stageDir == Path.of("/scratch")
        }

        and:
        with(paths) {
            inputDir.absolute
            outputDir.absolute
            stageDir.absolute
        }
    }

    def "File list mode normalizes user-defined input dir to an absolute dir"() {
        when:
        paths = new OrchestratorPaths.Builder(["input.ev"])
            .withInputDir(argDir)
            .build()

        then:
        paths.inputDir.absolute
        paths.inputDir == absoluteDir

        where:
        [argDir, absoluteDir] << toPaths(VALID_DIRS)
    }

    def "File list mode normalizes user-defined output dir to an absolute dir"() {
        when:
        paths = new OrchestratorPaths.Builder(["input.ev"])
            .withOutputDir(argDir)
            .build()

        then:
        paths.outputDir.absolute
        paths.outputDir == absoluteDir

        where:
        [argDir, absoluteDir] << toPaths(VALID_DIRS)
    }

    def "File list mode normalizes user-defined stage dir to an absolute dir"() {
        when:
        paths = new OrchestratorPaths.Builder(["input.ev"])
            .withStageDir(argDir)
            .build()

        then:
        paths.stageDir.absolute
        paths.stageDir == absoluteDir

        where:
        [argDir, absoluteDir] << toPaths(VALID_DIRS)
    }

    private static List<Path[]> toPaths(List<Map<String, String>> args) {
        args.collect { [Path.of(it.arg), Path.of(it.expectedDir).toAbsolutePath()] as Path[] }
    }

    static class OrchestratorPathsExtensions {
        static List<String> getInputNames(OrchestratorPaths paths) {
            paths.allFiles*.inputName
        }

        static List<String> getOutputNames(OrchestratorPaths paths) {
            paths.allFiles*.outputName
        }
    }
}
