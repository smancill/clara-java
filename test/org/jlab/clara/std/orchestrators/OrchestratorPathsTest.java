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

package org.jlab.clara.std.orchestrators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jlab.clara.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OrchestratorPathsTest {

    private static final List<String> SIMPLE_LIST = Arrays.asList("in.ev");

    @BeforeEach
    public void setUp() throws Exception {
    }

    @Test
    public void singleFileIsUsedAsInputFile() throws Exception {
        OrchestratorPaths paths = new OrchestratorPaths.Builder("input.ev", "out.ev").build();

        assertThat(inputFiles(paths), is(Arrays.asList("input.ev")));
        assertThat(paths.inputDir, is(Paths.get("").toAbsolutePath()));
        assertThat(paths.numFiles(), is(1));
    }

    @Test
    public void singleFileIsUsedAsOutputFile() throws Exception {
        OrchestratorPaths paths = new OrchestratorPaths.Builder("input.ev", "out.ev").build();

        assertThat(outputFiles(paths), is(Arrays.asList("out.ev")));
        assertThat(paths.outputDir, is(Paths.get("").toAbsolutePath()));
    }

    @Test
    public void singleFileCanContainPath() throws Exception {
        OrchestratorPaths paths = new OrchestratorPaths.Builder("/a/b/in.ev", "/t/out.ev").build();

        assertThat(inputFiles(paths), is(Arrays.asList("in.ev")));
        assertThat(paths.inputDir.toString(), is("/a/b"));
        assertThat(paths.outputDir.toString(), is("/t"));
    }

    @Test
    public void fileListIsUsedAsInputFiles() throws Exception {
        List<String> files = Arrays.asList("f1.ev", "f2.ev", "f3.ev");

        OrchestratorPaths paths = new OrchestratorPaths.Builder(files).build();

        assertThat(inputFiles(paths), is(files));
        assertThat(paths.numFiles(), is(3));
    }

    @Test
    public void fileListIsUsedAsOutputFiles() throws Exception {
        List<String> files = Arrays.asList("c1.evio", "c2.evio");

        OrchestratorPaths paths = new OrchestratorPaths.Builder(files).build();

        assertThat(outputFiles(paths), is(Arrays.asList("out_c1.evio", "out_c2.evio")));
    }

    @Test
    public void fileListShallNotContainFullPaths() throws Exception {
        List<String> files = Arrays.asList("/a/b/c1.evio", "/a/b/c2.evio");

        Exception ex = assertThrows(OrchestratorConfigException.class, () ->
                new OrchestratorPaths.Builder(files));
        assertThat(ex.getMessage(), is("Input file cannot be a path: /a/b/c1.evio"));
    }

    @Test
    public void fileListShallNotContainRelavitePaths() throws Exception {
        List<String> files = Arrays.asList("c1.evio", "b/c2.evio");

        Exception ex = assertThrows(OrchestratorConfigException.class, () ->
                new OrchestratorPaths.Builder(files));
        assertThat(ex.getMessage(), is("Input file cannot be a path: b/c2.evio"));
    }

    @Test
    public void defaultInputDirIsInClaraHome() throws Exception {
        OrchestratorPaths paths = new OrchestratorPaths.Builder(SIMPLE_LIST).build();

        assertThat(paths.inputDir, is(FileUtils.claraPath("data", "input")));
    }

    @Test
    public void defaultOutputDirIsInClaraHome() throws Exception {
        OrchestratorPaths paths = new OrchestratorPaths.Builder(SIMPLE_LIST).build();

        assertThat(paths.outputDir, is(FileUtils.claraPath("data", "output")));
    }

    @Test
    public void defaultStageDirIsInScratch() throws Exception {
        OrchestratorPaths paths = new OrchestratorPaths.Builder(SIMPLE_LIST).build();

        assertThat(paths.stageDir, is(Paths.get("/scratch")));
    }

    @Test
    public void inputDirCanBeUserDefined() throws Exception {
        OrchestratorPaths paths = new OrchestratorPaths.Builder(SIMPLE_LIST)
                .withInputDir("/data/files/input")
                .build();

        assertThat(paths.inputDir, is(Paths.get("/data/files/input")));
    }

    @Test
    public void outputDirCanBeUserDefined() throws Exception {
        OrchestratorPaths paths = new OrchestratorPaths.Builder(SIMPLE_LIST)
                .withOutputDir("/data/files/output")
                .build();

        assertThat(paths.outputDir, is(Paths.get("/data/files/output")));
    }

    @Test
    public void stageDirCanBeUserDefined() throws Exception {
        OrchestratorPaths paths = new OrchestratorPaths.Builder(SIMPLE_LIST)
                .withStageDir("/tmp/clara/stage")
                .build();

        assertThat(paths.stageDir, is(Paths.get("/tmp/clara/stage")));
    }

    private static List<String> inputFiles(OrchestratorPaths paths) {
        return paths.allFiles.stream().map(f -> f.inputName).collect(Collectors.toList());
    }

    private static List<String> outputFiles(OrchestratorPaths paths) {
        return paths.allFiles.stream().map(f -> f.outputName).collect(Collectors.toList());
    }
}
