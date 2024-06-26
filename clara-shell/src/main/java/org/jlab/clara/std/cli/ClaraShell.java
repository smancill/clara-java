/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import org.jlab.clara.util.ArgUtils;
import org.jlab.clara.util.FileUtils;
import org.jlab.clara.util.VersionUtils;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An interactive shell to run Clara DPEs and orchestrators.
 */
public final class ClaraShell implements AutoCloseable {

    private static final String HISTORY_NAME = ".clara_history";
    private static final String DEFAULT_PROMPT = defaultPrompt();

    private final Terminal terminal;
    private final Config config;

    private final Map<String, Command> commands;
    private final CommandRunner commandRunner;

    private final LineReader reader;
    private final History history;

    private final boolean interactive;
    private final Path script;

    private final Thread interactiveThread;
    private volatile boolean running;


    public static void main(String[] args) {
        var builder = ClaraShell.newBuilder();
        if (args.length == 1) {
            if (args[0].equals("--version")) {
                System.out.println(VersionUtils.getClaraVersionFull());
                System.exit(0);
            }
            if (args[0].equals("--help")) {
                System.err.println("usage: clara-shell [ <script> ]");
                System.exit(0);
            }
            builder.withScript(Path.of(args[0]));
        } else if (args.length > 1) {
            System.err.println("usage: clara-shell [ <script> ]");
            System.exit(1);
        }

        if (FarmCommands.hasPlugin()) {
            FarmCommands.register(builder);
        }

        var shell = builder.build();

        Runtime.getRuntime().addShutdownHook(new Thread(shell::close));

        shell.run();
    }


    /**
     * Creates a new builder of a Clara shell instance.
     *
     * @return the builder
     */
    public static Builder newBuilder() {
        return new Builder(TerminalBuilder.builder());
    }


    /**
     * Helps configuring and creating a new {@link ClaraShell}.
     */
    public static class Builder {

        private final TerminalBuilder terminal;
        private final Config.Builder config;

        private final List<CommandFactory> runSubCommands = new ArrayList<>();
        private final List<CommandFactory> editSubCommands = new ArrayList<>();

        private final List<CommandFactory> userCommands = new ArrayList<>();

        private Path script = null;
        private boolean interactive = true;

        /**
         * Creates a new builder.
         *
         * @param termBuilder the builder of the virtual terminal
         */
        public Builder(TerminalBuilder termBuilder) {
            this.terminal = termBuilder;
            this.config = new Config.Builder();
        }

        /**
         * Customizes the configuration variables of the shell session.
         *
         * @param consumer a function that updates the configuration builder
         * @return this builder
         */
        public Builder withConfiguration(Consumer<Config.Builder> consumer) {
            ArgUtils.requireNonNull(consumer, "configure function");

            consumer.accept(config);
            return this;
        }

        /**
         * Adds a new subcommand to the {@code run} builtin command.
         * This new subcommand cannot have the same name as one of the default
         * subcommands.
         *
         * @param factory the factory to create the subcommand
         * @return this builder
         */
        public Builder withRunSubCommand(CommandFactory factory) {
            ArgUtils.requireNonNull(factory, "command factory");

            runSubCommands.add(factory);
            return this;
        }

        /**
         * Adds a new subcommand to the {@code edit} builtin command.
         *
         * @param name the name of the subcommand
         * @param description the help description for the command
         * @param fileArg the path to the text file to be edited
         * @return this builder
         */
        public Builder withEditSubCommand(String name,
                                          String description,
                                          Function<Config, Path> fileArg) {
            ArgUtils.requireNonEmpty(name, "name");
            ArgUtils.requireNonEmpty(description, "description");
            ArgUtils.requireNonNull(fileArg, "edit subcommand argument");

            editSubCommands.add(EditCommand.newArgument(name, description, fileArg));
            return this;
        }

        /**
         * Adds a new builtin command to the Clara shell session.
         * This new command cannot have the same name as one of the default
         * builtin commands.
         *
         * @param name the name of the command
         * @param description the help description for the command
         * @param command the list containing the program and its arguments
         * @return this builder
         */
        public Builder withBuiltinCommand(String name, String description, List<String> command) {
            return withBuiltinCommand(name, description, command.toArray(new String[0]));
        }

        /**
         * Adds a new builtin command to the Clara shell session.
         * This new command cannot have the same name as one of the default
         * builtin commands.
         *
         * @param name the name of the command
         * @param description the help description for the command
         * @param command a string array containing the program and its arguments
         * @return this builder
         */
        public Builder withBuiltinCommand(String name, String description, String... command) {
            ArgUtils.requireNonEmpty(name, "name");
            ArgUtils.requireNonEmpty(description, "description");
            ArgUtils.requireNonNull(description, "command");

            userCommands.add(AbstractCommand.wrap(name, description, command));
            return this;
        }

        /**
         * Adds a new builtin command to the Clara shell session.
         * This new command cannot have the same name as one of the default
         * builtin commands.
         *
         * @param factory the factory to create the command
         * @return this builder
         */
        public Builder withBuiltinCommand(CommandFactory factory) {
            ArgUtils.requireNonNull(factory, "command factory");
            userCommands.add(factory);
            return this;
        }

        /**
         * Sets the script to be executed by the shell in non-interactive mode.
         *
         * @param script the script with commands to be executed by the shell
         * @return this builder
         */
        public Builder withScript(Path script) {
            ArgUtils.requireNonNull(script, "script");
            this.script = script;
            this.interactive = false;
            return this;
        }

        /**
         * Creates the user-interactive Clara shell instance.
         *
         * @return the created shell
         */
        public ClaraShell build() {
            return new ClaraShell(this);
        }
    }


    private ClaraShell(Builder builder) {
        try {
            terminal = builder.terminal.build();
            config = builder.config.build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        commands = new LinkedHashMap<>();
        commandRunner = new CommandRunner(terminal, commands);
        initCommands(builder);

        reader = LineReaderBuilder.builder()
                .completer(initCompleter())
                .parser(commandRunner.getParser())
                .terminal(terminal)
                .build();
        history = new DefaultHistory(reader);
        loadHistory();

        script = builder.script;
        interactive = builder.interactive;
        interactiveThread = new Thread(this::internalRun);
    }

    private void initCommands(Builder builder) {
        addCommand(initCommand(RunCommand::new, builder.runSubCommands));
        addCommand(initCommand(EditCommand::new, builder.editSubCommands));

        builder.userCommands.forEach(this::addCommand);

        addCommand(SetCommand::new);
        addCommand(ShowCommand::new);
        addCommand(SaveCommand::new);

        addCommand(s -> new SourceCommand(s, commandRunner));
        addCommand(s -> new HelpCommand(s, commands));
    }

    private CommandFactory initCommand(CommandFactory baseCommand,
                                       List<CommandFactory> userSubCommands) {
        return session -> {
            var cmd = (BaseCommand) baseCommand.create(session);
            userSubCommands.forEach(cmd::addSubCommand);
            return cmd;
        };
    }

    private void addCommand(CommandFactory factory) {
        var command = factory.create(new Context(terminal, config));
        commands.put(command.getName(), command);
    }

    private Completer initCompleter() {
        var completers = commands.values()
                .stream()
                .map(Command::getCompleter)
                .collect(Collectors.toList());
        return new AggregateCompleter(completers);
    }

    private void loadHistory() {
        var histFile = FileUtils.claraPath(HISTORY_NAME);
        reader.setVariable(LineReader.HISTORY_FILE, histFile);
        try {
            history.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            running = false;
            interactiveThread.interrupt();
            interactiveThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (var command : commands.values()) {
            try {
                command.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            history.save();
            terminal.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs the shell accepting user commands.
     * If a script file is set, then the shell will process the commands in the
     * script in non-interactive mode and exit. Otherwise, an interactive
     * session will be started.
     */
    public void run() {
        if (interactive) {
            printWelcomeMessage(terminal.writer());
        }
        if (script !=  null) {
            try {
                var args = new String[] {"-q", script.toString()};
                var sourceCmd = commands.get("source");
                sourceCmd.execute(args);
            } finally {
                terminal.writer().flush();
            }
        }
        if (interactive) {
            interactiveThread.start();
        }
    }

    private void internalRun() {
        running = true;
        while (running) {
            try {
                Thread.interrupted();
                var line = readLine();
                if (line == null) {
                    continue;
                }
                commandRunner.execute(line);
            } catch (EndOfFileException e) {
                break;
            } catch (UserInterruptException e) {
                continue;
            } finally {
                terminal.flush();
            }
        }
    }

    private void printWelcomeMessage(PrintWriter writer) {
        var version = VersionUtils.getClaraVersion();

        writer.println();
        writer.println("   ██████╗██╗      █████╗ ██████╗  █████╗ ");
        writer.println("  ██╔════╝██║     ██╔══██╗██╔══██╗██╔══██╗  " + version);
        writer.println("  ██║     ██║     ███████║██████╔╝███████║");
        writer.println("  ██║     ██║     ██╔══██║██╔══██╗██╔══██║");
        writer.println("  ╚██████╗███████╗██║  ██║██║  ██║██║  ██║");
        writer.println("   ╚═════╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝");
        writer.println();
        writer.println();
        writer.println(" Run 'help' to show available commands.");
    }

    private String readLine() {
        return reader.readLine(DEFAULT_PROMPT);
    }

    private static String defaultPrompt() {
        return new AttributedStringBuilder()
                .append('\n')
                .style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
                .append("clara>")
                .style(AttributedStyle.DEFAULT)
                .append(' ')
                .toAnsi();
    }
}
