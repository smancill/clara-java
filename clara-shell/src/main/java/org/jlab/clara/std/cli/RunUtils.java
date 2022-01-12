/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import org.jlab.clara.base.ClaraLang;
import org.jlab.clara.base.DataRingAddress;
import org.jlab.clara.base.DpeName;
import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.util.EnvUtils;
import org.jlab.clara.util.FileUtils;
import org.jline.builtins.Commands;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class RunUtils {

    private final Config config;

    RunUtils(Config config) {
        this.config = config;
    }

    Optional<String> getMonitorFrontEnd() {
        if (config.hasValue(Config.MONITOR_HOST)) {
            var monAddr = new DataRingAddress(config.getString(Config.MONITOR_HOST));
            var monDpe = new DpeName(monAddr.host(), monAddr.port(), ClaraLang.JAVA);
            return Optional.of(monDpe.canonicalName());
        }
        return EnvUtils.get(ClaraConstants.ENV_MONITOR_FE)
                       .map(DpeName::new)
                       .map(DpeName::canonicalName);
    }

    String getSession() {
        var sessionId = config.getString(Config.SESSION);
        var sessionDesc = config.getString(Config.DESCRIPTION);
        return sessionId + "_" + sessionDesc;
    }

    Path getLogDir() {
        return config.getPath(Config.LOG_DIR);
    }

    Path getLogFile(DpeName name) {
        var lang = name.language();
        var component = (lang == ClaraLang.JAVA) ? "fe_dpe" : lang + "_dpe";
        return getLogFile(name.address().host(), component);
    }

    Path getLogFile(String host, String component) {
        var name = String.format("%s_%s_%s.log", host, getSession(), component);
        return getLogDir().resolve(name);
    }

    Path getLogFile(Path feLog, ClaraLang dpeLang) {
        var name = FileUtils.getFileName(feLog).toString();
        return getLogDir().resolve(name.replaceAll("fe_dpe", dpeLang + "_dpe"));
    }

    private static int compareModificationTime(Path a, Path b) {
        try {
            return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    List<Path> getLogFiles(String component) throws IOException {
        var dir = getLogDir();
        var glob = String.format("glob:*_%s_%s.log", getSession(), component);
        var matcher = dir.getFileSystem().getPathMatcher(glob);

        try (var files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                        .filter(p -> matcher.matches(p.getFileName()))
                        .sorted(RunUtils::compareModificationTime)
                        .toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    static int printFile(Terminal terminal, Path path) {
        if (!Files.exists(path)) {
            terminal.writer().printf("error: no file %s%n", path);
            return Command.EXIT_ERROR;
        }
        try (var lines = Files.lines(path)) {
            lines.forEach(terminal.writer()::println);
            return Command.EXIT_SUCCESS;
        } catch (IOException e) {
            terminal.writer().printf("error: could not open file: %s%n", e);
            return Command.EXIT_ERROR;
        } catch (UncheckedIOException e) {
            terminal.writer().printf("error: could not read file: %s%n", e);
            return Command.EXIT_ERROR;
        }
    }

    static int paginateFile(Terminal terminal, String description, Path... paths) {
        for (var path : paths) {
            if (!Files.exists(path)) {
                terminal.writer().printf("error: no %s log: %s%n", description, path);
                return Command.EXIT_ERROR;
            }
        }
        try {
            var args = Arrays.stream(paths).map(Path::toString).toArray(String[]::new);
            Commands.less(terminal, System.in, System.out, System.err, Path.of(""), args);
            return Command.EXIT_SUCCESS;
        } catch (IOException e) {
            terminal.writer().printf("error: could not open %s log: %s%n", description, e);
            return Command.EXIT_ERROR;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Command.EXIT_ERROR;
        } catch (Exception e) {
            terminal.writer().printf("error: %s%n", e);
            return Command.EXIT_ERROR;
        }
    }

    static int listFiles(Path dir, String opts) {
        return CommandUtils.runProcess("ls", "-" + opts, dir.toString());
    }
}
