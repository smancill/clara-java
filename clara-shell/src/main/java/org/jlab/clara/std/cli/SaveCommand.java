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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

class SaveCommand extends AbstractCommand {

    SaveCommand(Context context) {
        super(context, "save", "Export configuration to file");
    }

    @Override
    public int execute(String[] args) {
        if (args.length < 1) {
            writer.println("Error: missing filename argument");
            return EXIT_ERROR;
        }
        Path path = FileUtils.expandHome(args[0]);
        if (Files.exists(path)) {
            boolean overwrite = scanAnswer();
            if (!overwrite) {
                writer.println("The config was not saved");
                return EXIT_SUCCESS;
            }
        }
        return writeFile(path);
    }

    private boolean scanAnswer() {
        Scanner scan = new Scanner(System.in);
        while (true) {
            System.out.print("The file already exists. Do you want to overwrite it? (y/N): ");
            String answer = scan.nextLine();
            switch (answer) {
                case "y":
                case "Y":
                case "yes":
                case "Yes":
                    return true;
                case "n":
                case "N":
                case "no":
                case "No":
                case "":
                    return false;
                default:
                    System.out.println("Invalid answer.");
            }
        }
    }

    private int writeFile(Path path) {
        try (PrintWriter printer = FileUtils.openOutputTextFile(path, false)) {
            for (ConfigVariable variable : config.getVariables()) {
                if (variable.hasValue()) {
                    printer.printf("set %s %s%n", variable.getName(), variable.getValue());
                }
            }
        } catch (IOException e) {
            writer.printf("Error: could not write file: %s: %s%n", path, e.getMessage());
            return EXIT_ERROR;
        }
        writer.println("Config saved in " + path);
        return EXIT_SUCCESS;
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
