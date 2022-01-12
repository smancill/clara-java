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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class OrchestratorPaths {

    static final Path INPUT_DIR = FileUtils.claraPath("data", "input");
    static final Path OUTPUT_DIR = FileUtils.claraPath("data", "output");
    static final Path STAGE_DIR = Path.of(File.separator + "scratch");

    final List<FileInfo> allFiles;

    final Path inputDir;
    final Path outputDir;
    final Path stageDir;


    static class Builder {

        private final List<FileInfo> allFiles;

        private Path inputDir = INPUT_DIR;
        private Path outputDir = OUTPUT_DIR;
        private Path stageDir = STAGE_DIR;

        Builder(Path inputFile, Path outputFile) {
            Objects.requireNonNull(inputFile, "inputFile parameter is null");
            Objects.requireNonNull(outputFile, "outputFile parameter is null");

            var inputName = FileUtils.getFileName(inputFile).toString();
            var outputName = FileUtils.getFileName(outputFile).toString();

            this.allFiles = List.of(new FileInfo(inputName, outputName));
            this.inputDir = toFullPath(FileUtils.getParent(inputFile));
            this.outputDir = toFullPath(FileUtils.getParent(outputFile));
        }

        Builder(List<String> inputFiles) {
            Objects.requireNonNull(inputFiles, "inputFiles parameter is null");
            if (inputFiles.isEmpty()) {
                throw new IllegalArgumentException("inputFiles list is empty");
            }
            this.allFiles = inputFiles.stream()
                    .peek(f -> checkValidFileName(f))
                    .map(f -> new FileInfo(f, "out_" + f))
                    .collect(Collectors.toList());
        }

        Builder withInputDir(Path inputDir) {
            Objects.requireNonNull(inputDir, "inputDir parameter is null");
            this.inputDir = toFullPath(inputDir);
            return this;
        }

        Builder withOutputDir(Path outputDir) {
            Objects.requireNonNull(outputDir, "outputDir parameter is null");
            this.outputDir = toFullPath(outputDir);
            return this;
        }

        Builder withStageDir(Path stageDir) {
            Objects.requireNonNull(stageDir, "stageDir parameter is null");
            this.stageDir = toFullPath(stageDir);
            return this;
        }

        OrchestratorPaths build() {
            return new OrchestratorPaths(this);
        }

        private static void checkValidFileName(String file) {
            try {
                if (Path.of(file).getParent() != null) {
                    throw new OrchestratorConfigException("Input file cannot be a path: " + file);
                }
            } catch (InvalidPathException e) {
                throw new OrchestratorConfigException(e);
            }
        }

        private static Path toFullPath(Path arg) {
            return arg.toAbsolutePath().normalize();
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
