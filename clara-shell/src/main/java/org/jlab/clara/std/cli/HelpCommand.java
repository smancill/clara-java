/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jlab.clara.base.ClaraUtil;
import org.jline.builtins.Less;
import org.jline.builtins.Source;

class HelpCommand extends BaseCommand {

    private final Map<String, Command> commands;

    HelpCommand(Context context, Map<String, Command> commands) {
        super(context, "help", "Display help information about CLARA shell");
        this.commands = commands;
        addCommands();
    }

    @Override
    public int execute(String[] args) {
        if (args.length < 1) {
            writer.println("Commands:\n");
            subCommands.values().stream()
                       .map(Command::getName)
                       .forEach(this::printCommand);
            writer.println("\nUse help <command> for details about each command.");
            return EXIT_SUCCESS;
        }

        Command command = commands.get(args[0]);
        if (command == null) {
            writer.println("Invalid command name.");
            return EXIT_ERROR;
        }
        return showHelp(command);
    }

    private void printCommand(String name) {
        Command command = commands.get(name);
        writer.printf("   %-14s", command.getName());
        writer.printf("%s%n", command.getDescription());
    }

    private void addCommands() {
        commands.values().stream()
                .filter(c -> !c.getName().equals("help"))
                .forEach(c -> addSubCommand(c.getName(), args -> 0, c.getDescription()));
    }

    private int showHelp(Command command) {
        try {
            String help = getHelp(command);
            if (terminal.getHeight() - 2 > countLines(help)) {
                writer.print(help);
            } else {
                Less less = new Less(terminal, Paths.get(""));
                List<Source> sources = new ArrayList<>();
                sources.add(new Source() {
                    @Override
                    public String getName() {
                        return "help " + command.getName();
                    }

                    @Override
                    public InputStream read() {
                        String text = String.format("help %s%n%s%n", command.getName(), help);
                        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
                    }

                    @Override
                    public Long lines() {
                        return help.lines().count();
                    }
                });
                // Less.run(Sources...) is bugged and passes an unmodifiable list to
                // Less.run(List<Sources>), which then modifies the list, and of course it throws.
                less.run(sources);
            }
            return EXIT_SUCCESS;
        } catch (IOException e) {
            writer.print("Error: could not show help: " + e.getMessage());
            return EXIT_ERROR;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return EXIT_ERROR;
        }
    }

    private String getHelp(Command command) {
        StringWriter helpWriter = new StringWriter();
        PrintWriter printer = new PrintWriter(helpWriter);
        command.printHelp(printer);
        printer.close();
        return helpWriter.toString();
    }

    private int countLines(String str) {
        // TODO it could be faster
        String[] lines = str.split("\r\n|\r|\n");
        return lines.length;
    }

    @Override
    public void printHelp(PrintWriter printer) {
        String help = "Show help for the command.";
        printer.printf("%n  %s <command>%n", name);
        printer.printf("%s%n", ClaraUtil.splitIntoLines(help, "    ", 72));
    }
}
