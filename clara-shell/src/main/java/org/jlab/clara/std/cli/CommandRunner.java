/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import org.jlab.clara.util.EnvUtils;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;

import java.util.Arrays;
import java.util.Map;

class CommandRunner {

    private final Terminal terminal;
    private final Parser parser;
    private final Map<String, Command> commands;

    CommandRunner(Terminal terminal, Map<String, Command> commands) {
        this.terminal = terminal;
        this.parser = new DefaultParser();
        this.commands = commands;
    }

    public int execute(String line) {
        String[] shellArgs = parseLine(line);
        if (shellArgs == null) {
            return Command.EXIT_ERROR;
        }
        if (shellArgs.length == 0) {
            return Command.EXIT_SUCCESS;
        }
        String name = shellArgs[0];
        Command command = commands.get(name);
        if (command == null) {
            if ("exit".equals(name)) {
                throw new EndOfFileException();
            }
            terminal.writer().println("Invalid command");
            return Command.EXIT_ERROR;
        }
        Thread execThread = Thread.currentThread();
        SignalHandler prevIntHandler = terminal.handle(Signal.INT, s -> execThread.interrupt());
        try {
            String[] args = Arrays.copyOfRange(shellArgs, 1, shellArgs.length);
            return command.execute(args);
        } finally {
            terminal.handle(Signal.INT, prevIntHandler);
            terminal.writer().flush();
        }
    }

    private String[] parseLine(String line) {
        try {
            String cmdLine = EnvUtils.expandEnvironment(line, System.getenv()).trim();
            return parser.parse(cmdLine, cmdLine.length() + 1)
                         .words()
                         .toArray(new String[0]);
        } catch (IllegalArgumentException e) {
            terminal.writer().println(e.getMessage());
            return null;
        }
    }

    Parser getParser() {
        return parser;
    }
}
