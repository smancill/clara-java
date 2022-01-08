/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.util.FileUtils;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

class OrchestratorPaths {

    static final String INPUT_DIR = FileUtils.claraPath("data", "input").toString();
    static final String OUTPUT_DIR = FileUtils.claraPath("data", "output").toString();
    static final String STAGE_DIR = File.separator + "scratch";

    final List<FileInfo> allFiles;

    final Path inputDir;
    final Path outputDir;
    final Path stageDir;


    static class Builder {

        private final List<FileInfo> allFiles;

        private Path inputDir = Paths.get(INPUT_DIR);
        private Path outputDir = Paths.get(OUTPUT_DIR);
        private Path stageDir = Paths.get(STAGE_DIR);

        Builder(String inputFile, String outputFile) {
            Path inputPath = Paths.get(inputFile);
            Path outputPath = Paths.get(outputFile);

            String inputName = FileUtils.getFileName(inputPath).toString();
            String outputName = FileUtils.getFileName(outputPath).toString();

            this.allFiles = List.of(new FileInfo(inputName, outputName));
            this.inputDir = FileUtils.getParent(inputPath).toAbsolutePath().normalize();
            this.outputDir = FileUtils.getParent(outputPath).toAbsolutePath().normalize();
        }

        Builder(List<String> inputFiles) {
            this.allFiles = inputFiles.stream()
                    .peek(f -> checkValidFileName(f))
                    .map(f -> new FileInfo(f, "out_" + f))
                    .collect(Collectors.toList());
        }

        Builder withInputDir(String inputDir) {
            this.inputDir = Paths.get(inputDir).toAbsolutePath().normalize();
            return this;
        }

        Builder withOutputDir(String outputDir) {
            this.outputDir = Paths.get(outputDir).toAbsolutePath().normalize();
            return this;
        }

        Builder withStageDir(String stageDir) {
            this.stageDir = Paths.get(stageDir).toAbsolutePath().normalize();
            return this;
        }

        OrchestratorPaths build() {
            return new OrchestratorPaths(this);
        }

        private static void checkValidFileName(String file) {
            try {
                if (Paths.get(file).getParent() != null) {
                    throw new OrchestratorConfigException("Input file cannot be a path: " + file);
                }
            } catch (InvalidPathException e) {
                throw new OrchestratorConfigException(e);
            }
        }
    }


    protected OrchestratorPaths(Builder builder) {
        this.allFiles = builder.allFiles;
        this.inputDir = builder.inputDir;
        this.outputDir = builder.outputDir;
        this.stageDir = builder.stageDir;
    }

    Path inputFilePath(FileInfo file) {
        return inputDir.resolve(file.inputName);
    }

    Path outputFilePath(FileInfo file) {
        return outputDir.resolve(file.outputName);
    }

    Path stageInputFilePath(FileInfo file) {
        return stageDir.resolve(file.inputName);
    }

    Path stageOutputFilePath(FileInfo file) {
        return stageDir.resolve(file.outputName);
    }

    int numFiles() {
        return allFiles.size();
    }
}
