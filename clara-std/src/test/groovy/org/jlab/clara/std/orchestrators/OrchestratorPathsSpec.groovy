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

package org.jlab.clara.std.orchestrators

import org.jlab.clara.util.FileUtils
import spock.lang.Rollup
import spock.lang.Specification
import spock.lang.Subject
import spock.util.mop.Use

import java.nio.file.Path
import java.nio.file.Paths

@Rollup
class OrchestratorPathsSpec extends Specification {

    private static final WORKING_DIR = Paths.get("").toAbsolutePath()

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
        paths = new OrchestratorPaths.Builder(inputFile.toString(), "out.ev").build()

        then:
        paths.inputNames == [inputFile.fileName.toString()]
        paths.numFiles() == 1

        where:
        [inputFile, _] << toPaths(VALID_FILES)
    }

    def "Single file mode sets an absolute normalized input dir based on input file path"() {
        when:
        paths = new OrchestratorPaths.Builder(inputFile.toString(), "out.ev").build()

        then:
        paths.inputDir.absolute
        paths.inputDir == inputDir

        where:
        [inputFile, inputDir] << toPaths(VALID_FILES)
    }

    @Use(OrchestratorPathsExtensions)
    def "Single file mode requires a single output file path"() {
        when:
        paths = new OrchestratorPaths.Builder("input.ev", outputFile.toString()).build()

        then:
        paths.outputNames == [outputFile.fileName.toString()]

        where:
        [outputFile, _] << toPaths(VALID_FILES)
    }

    def "Single file mode sets an absolute normalized output dir based on output file path"() {
        when:
        paths = new OrchestratorPaths.Builder("input.ev", outputFile.toString()).build()

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
            stageDir == Paths.get("/scratch")
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
            .withInputDir(argDir.toString())
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
            .withOutputDir(argDir.toString())
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
            .withStageDir(argDir.toString())
            .build()

        then:
        paths.stageDir.absolute
        paths.stageDir == absoluteDir

        where:
        [argDir, absoluteDir] << toPaths(VALID_DIRS)
    }

    private static List<Path[]> toPaths(List<Map<String, String>> args) {
        args.collect { [Paths.get(it.arg), WORKING_DIR.resolve(it.expectedDir)] }
    }

    static class OrchestratorPathsExtensions {
        static List<String> getInputNames(OrchestratorPaths paths) {
            paths.allFiles.collect { it.inputName }
        }

        static List<String> getOutputNames(OrchestratorPaths paths) {
            paths.allFiles.collect { it.outputName }
        }
    }
}
