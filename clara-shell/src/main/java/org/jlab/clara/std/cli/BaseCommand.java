/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import org.jlab.clara.base.ClaraUtil;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A command that groups a list of subcommands that can be executed separately.
 */
public abstract class BaseCommand extends AbstractCommand {

    /**
     * The supported subcommands.
     */
    protected final Map<String, Command> subCommands;

    /**
     * Creates a new base command to group subcommands.
     *
     * @param context the context of the shell session
     * @param name the name of the base command
     * @param description the description of the base command
     */
    protected BaseCommand(Context context, String name, String description) {
        super(context, name, description);
        this.subCommands = new LinkedHashMap<>();
    }

    @Override
    public int execute(String[] args) {
        if (args.length < 1) {
            writer.println("Error: missing argument(s).");
            return EXIT_ERROR;
        }
        var subName = args[0];
        var subCmd = subCommands.get(subName);
        if (subCmd == null) {
            writer.println("Error: unknown argument " + subName);
            return EXIT_ERROR;
        }
        try {
            var cmdArgs = Arrays.copyOfRange(args, 1, args.length);
            return subCmd.execute(cmdArgs);
        } catch (IllegalArgumentException e) {
            writer.println("Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return EXIT_ERROR;
    }

    /**
     * Creates and adds a new subcommand to the list of subcommands supported by
     * this command.
     *
     * @param name the name of the subcommand
     * @param action the action to run when the subcommand is executed
     * @param description the description of the subcommand
     * @throws IllegalArgumentException if a subcommand of the given name already exists
     */
    protected void addSubCommand(String name,
                                 Function<String[], Integer> action,
                                 String description) {
        addSubCommand(session -> new AbstractCommand(session, name, description) {
            @Override
            public int execute(String[] args) {
                return action.apply(args);
            }
        });
    }

    /**
     * Adds the given subcommand to the list of subcommands supported by this
     * command.
     *
     * @param factory the factory for a new subcommand of this command
     * @throws IllegalArgumentException if a subcommand of the given name already exists
     */
    protected void addSubCommand(CommandFactory factory) {
        var subCmd = factory.create(new Context(terminal, config));
        var subName = subCmd.getName();
        var prev = subCommands.putIfAbsent(subName, subCmd);
        if (prev != null) {
            var error = String.format("a subcommand '%s %s' already exists", name, subName);
            throw new IllegalArgumentException(error);
        }
    }

    @Override
    public Completer getCompleter() {
        var completers = subCommands.values()
                .stream()
                .map(this::getCompleter)
                .collect(Collectors.toList());
        return new AggregateCompleter(completers);
    }

    private Completer getCompleter(Command subCmd) {
        var allCompleters = new ArrayList<Completer>();
        allCompleters.add(new StringsCompleter(name));

        var subCompleter = subCmd.getCompleter();
        if (subCompleter instanceof ArgumentCompleter argCompleter) {
            allCompleters.addAll(argCompleter.getCompleters());
        } else {
            allCompleters.add(subCompleter);
            allCompleters.add(NullCompleter.INSTANCE);
        }

        return new ArgumentCompleter(allCompleters);
    }

    @Override
    public void printHelp(PrintWriter printer) {
        for (var subCmd: subCommands.values()) {
            printer.printf("%n  %s %s%n", name, subCmd.getName());
            printer.printf("%s%n", ClaraUtil.splitIntoLines(subCmd.getDescription(), "    ", 72));
        }
    }

    @Override
    public void close() throws Exception {
        for (var subCmd : subCommands.values()) {
            try {
                subCmd.close();
            } catch (Exception e) {
                writer.println(e.getMessage());
            }
        }
    }
}
