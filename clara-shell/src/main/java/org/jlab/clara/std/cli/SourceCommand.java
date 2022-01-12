/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.util.FileUtils;
import org.jline.builtins.Completers;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SourceCommand extends AbstractCommand {

    private final CommandRunner commandRunner;

    SourceCommand(Context context, CommandRunner commandRunner) {
        super(context, "source", "Read and execute commands from file");
        this.commandRunner = commandRunner;
    }

    @Override
    public int execute(String[] args) {
        boolean verbose = true;
        String sourceFile;

        if (args.length == 1) {
            sourceFile = args[0];
        } else if (args.length == 2) {
            if (args[0].equals("-q")) {
                verbose = false;
            } else {
                writer.println("Error: invalid number of arguments");
                return EXIT_ERROR;
            }
            sourceFile = args[1];
        } else {
            writer.println("Error: missing filename argument");
            return EXIT_ERROR;
        }

        var path = FileUtils.expandHome(sourceFile);
        try {
            for (var line : readLines(path)) {
                if (verbose) {
                    writer.println(line);
                }
                commandRunner.execute(line);
            }
            return EXIT_SUCCESS;
        } catch (NoSuchFileException e) {
            writer.println("Error: no such file: " + path);
            return EXIT_ERROR;
        } catch (IOException e) {
            writer.println("Error: could not read source file: " + e.getMessage());
            return EXIT_ERROR;
        } catch (UncheckedIOException e) {
            writer.println("Error: could not read source file: " + e.getCause().getMessage());
            return EXIT_ERROR;
        }
    }

    private static List<String> readLines(Path sourceFile) throws IOException {
        var pattern = Pattern.compile("^\\s*#.*$");
        try (var lines = Files.lines(sourceFile)) {
            return lines.filter(line -> !line.isEmpty())
                        .filter(line -> !pattern.matcher(line).matches())
                        .collect(Collectors.toList());
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    public Completer getCompleter() {
        Completer command = new StringsCompleter(getName());
        Completer fileCompleter = new Completers.FileNameCompleter();
        return new ArgumentCompleter(command, fileCompleter, NullCompleter.INSTANCE);
    }

    @Override
    public void printHelp(PrintWriter printer) {
        printer.printf("%n  %s <file_path>%n", name);
        printer.printf("%s.%n", ClaraUtil.splitIntoLines(description, "    ", 72));
    }
}
