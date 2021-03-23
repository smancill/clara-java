/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base.core;

import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.MimeType;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.ProxyAddress;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public final class CommandDebugger extends ClaraBase {

    private final Pattern commentPattern = Pattern.compile("^\\s*#.*$");
    private final Pattern sleepPattern = Pattern.compile("^\\s*(sleep)\\s+(\\d*)$");

    private CommandDebugger() {
        super(ClaraComponent.orchestrator("broker", 1, "broker"), ClaraComponent.dpe());
    }

    private void processFile(String file) {
        var path = Path.of(file);
        try (var commands = Files.lines(path, Charset.defaultCharset())) {
            commands.forEach(this::processCommand);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processCommand(String line) {
        line = line.trim();
        if (line.length() == 0) { // empty line
            return;
        }
        if (commentPattern.matcher(line).matches()) { // commented line
            return;
        }
        var sleep = sleepPattern.matcher(line);
        if (sleep.matches()) { // sleep command
            var time = Integer.parseInt(sleep.group(2));
            System.out.printf("Sleeping %d ms%n", time);
            ClaraUtil.sleep(time);
            return;
        }

        try {
            var cmd = new Command(line);
            System.out.println("C: " + cmd);
            try (var connection = getConnection(cmd.address)) {
                var message = MessageUtil.buildRequest(cmd.topic, cmd.request);
                message.getMetaData().setAuthor(getName());
                message.getMetaData().setSender(getName());
                if (cmd.action.equals("send")) {
                    publish(connection, message);
                } else {
                    printResponse(syncPublish(connection, message, cmd.timeout));
                }
            }
        } catch (ClaraMsgException | ClaraException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void printResponse(Message res) {
        var mimeType = res.getMimeType();
        if (mimeType.equals(MimeType.STRING)) {
            var data = new String(res.getData());
            System.out.printf("R: %s%n", data);
        } else {
            System.out.printf("R: mime-type = %s%n", mimeType);
        }
    }


    private static class Command {

        private final String action;
        private final ProxyAddress address;
        private final Topic topic;
        private final String request;

        private int timeout = 0;

        Command(String cmd) throws ClaraException {
            try {
                var tokenizer = new StringTokenizer(cmd, " ");
                action = tokenizer.nextToken();
                if (!action.equals("send") && !action.equals("sync_send")) {
                    throw new ClaraException("Invalid action: " + action);
                }
                if (action.equals("sync_send")) {
                    timeout = Integer.parseInt(tokenizer.nextToken());
                }
                var component = tokenizer.nextToken().replace("localhost", ClaraUtil.localhost());
                address = new ProxyAddress(ClaraUtil.getDpeHost(component),
                                           ClaraUtil.getDpePort(component));
                if (ClaraUtil.isDpeName(component)) {
                    topic = Topic.build("dpe", component);
                } else if (ClaraUtil.isContainerName(component)) {
                    topic = Topic.build("dpe", component);
                } else if (ClaraUtil.isServiceName(component)) {
                    topic = Topic.wrap(component);
                } else {
                    throw new ClaraException("Not a Clara component: " + component);
                }
                request = tokenizer.nextToken();
            } catch (NoSuchElementException | NumberFormatException e) {
                throw new RuntimeException("Invalid line: " + cmd, e);
            }
        }

        @Override
        public String toString() {
            var sb = new StringBuilder();
            sb.append("action = ").append(action).append(" ");
            if (timeout > 0)  {
                sb.append(" timeout = ").append(timeout).append(" ");
            }
            sb.append(" proxy = ").append(address).append(" ");
            sb.append(" topic = ").append(topic).append(" ");
            sb.append(" request = ").append(request);
            return sb.toString();
        }
    }


    public static void main(String[] args) {
        try (var broker = new CommandDebugger()) {
            broker.processFile(args[0]);
        }
    }
}
