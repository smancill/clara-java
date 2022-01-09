/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import org.jlab.clara.util.FileUtils;
import org.jline.builtins.Completers;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;

class SetCommand extends BaseCommand {

    SetCommand(Context context) {
        super(context, "set", "Parameter settings");
        setArguments();
    }

    private void setArguments() {
        var commands = new LinkedList<CommandFactory>();
        config.getVariables().stream().map(this::subCmd).forEach(commands::add);

        commands.add(1, subCmd("files", this::setFiles, new Completers.FileNameCompleter(),
                "Set the input files to be processed (example: /mnt/data/files/*.evio). "
                        + "This will set both fileList and inputDir variables."));

        commands.forEach(this::addSubCommand);
    }

    private CommandFactory subCmd(ConfigVariable v) {
        return subCmd(v.getName(), v::parseValue, v.getCompleter(), v.getDescription());
    }

    private CommandFactory subCmd(String name,
                                  Consumer<String[]> action,
                                  Completer completer,
                                  String description) {
        return session -> new AbstractCommand(session, name, description) {

            @Override
            public int execute(String[] args) {
                try {
                    action.accept(args);
                    return EXIT_SUCCESS;
                } catch (Exception e) {
                    writer.printf("Error: %s%n", e.getMessage());
                    return EXIT_ERROR;
                }
            }

            @Override
            public Completer getCompleter() {
                return new ArgumentCompleter(new StringsCompleter(name), completer);
            }
        };
    }

    private void setFiles(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("missing argument");
        }
        try {
            var path = FileUtils.expandHome(args[0]);
            var output = getOutputFile();
            try (var printer = FileUtils.openOutputTextFile(output.toPath(), false)) {
                if (Files.isDirectory(path)) {
                    listCommand(printer, args[0]);
                    var numFiles = listDir(printer, path, f -> true);
                    if (numFiles > 0) {
                        config.setValue(Config.INPUT_DIR, path.toString());
                        config.setValue(Config.FILES_LIST, output.getAbsolutePath());
                    } else {
                        throw new IllegalArgumentException("empty input directory");
                    }
                } else if (Files.isRegularFile(path)) {
                    listCommand(printer, args[0]);
                    printer.println(path.getFileName());
                    config.setValue(Config.INPUT_DIR, FileUtils.getParent(path).toString());
                    config.setValue(Config.FILES_LIST, output.getAbsolutePath());
                } else if (path.getFileName().toString().contains("*")
                        && Files.isDirectory(FileUtils.getParent(path))) {
                    listCommand(printer, args[0]);
                    var pattern = path.getFileName().toString();
                    var glob = "glob:" + pattern;
                    var matcher = FileSystems.getDefault().getPathMatcher(glob);
                    var numFiles = listDir(printer, FileUtils.getParent(path), matcher::matches);
                    if (numFiles > 0) {
                        config.setValue(Config.INPUT_DIR, FileUtils.getParent(path).toString());
                        config.setValue(Config.FILES_LIST, output.getAbsolutePath());
                    } else {
                        printer.println("# no files matched");
                        throw new IllegalArgumentException("no files matched");
                    }
                } else {
                    throw new IllegalArgumentException("invalid path");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getOutputFile() {
        var userDir = FarmCommands.hasPlugin()
                ? FarmCommands.PLUGIN.resolve("config")
                : Paths.get("");
        var runUtils = new RunUtils(config);
        var name = String.format("files_%s.txt", runUtils.getSession());
        return userDir.resolve(name).toFile();
    }

    private void listCommand(PrintWriter printer, String arg) {
        printer.printf("# auto-generated by: set files %s%n", arg);
    }

    private int listDir(PrintWriter printer, Path directory, Predicate<Path> filter)
            throws IOException {
        return (int) Files.list(directory)
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .filter(filter)
                .sorted()
                .peek(printer::println)
                .count();
    }
}
