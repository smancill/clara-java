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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        String sessionId = config.getString(Config.SESSION);
        String sessionDesc = config.getString(Config.DESCRIPTION);
        return sessionId + "_" + sessionDesc;
    }

    Path getLogDir() {
        return Paths.get(config.getString(Config.LOG_DIR));
    }

    Path getLogFile(DpeName name) {
        ClaraLang lang = name.language();
        String component = (lang == ClaraLang.JAVA) ? "fe_dpe" : lang + "_dpe";
        return getLogFile(name.address().host(), component);
    }

    Path getLogFile(String host, String component) {
        String name = String.format("%s_%s_%s.log", host, getSession(), component);
        return getLogDir().resolve(name);
    }

    Path getLogFile(Path feLog, ClaraLang dpeLang) {
        String name = FileUtils.getFileName(feLog).toString();
        return getLogDir().resolve(name.replaceAll("fe_dpe", dpeLang + "_dpe"));
    }

    List<Path> getLogFiles(String component) throws IOException {
        String glob = String.format("glob:*_%s_%s.log", getSession(), component);
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(glob);

        Function<Path, Long> modDate = path -> path.toFile().lastModified();

        return Files.list(getLogDir())
                    .filter(Files::isRegularFile)
                    .filter(p -> matcher.matches(p.getFileName()))
                    .sorted((a, b) -> Long.compare(modDate.apply(b), modDate.apply(a)))
                    .collect(Collectors.toList());
    }

    static int printFile(Terminal terminal, Path path) {
        if (!Files.exists(path)) {
            terminal.writer().printf("error: no file %s%n", path);
            return Command.EXIT_ERROR;
        }
        try {
            Files.lines(path).forEach(terminal.writer()::println);
            return Command.EXIT_SUCCESS;
        } catch (IOException e) {
            terminal.writer().printf("error: could not open file: %s%n", e);
            return Command.EXIT_ERROR;
        }
    }

    static int paginateFile(Terminal terminal, String description, Path... paths) {
        for (Path path : paths) {
            if (!Files.exists(path)) {
                terminal.writer().printf("error: no %s log: %s%n", description, path);
                return Command.EXIT_ERROR;
            }
        }
        try {
            String[] args = Arrays.stream(paths).map(Path::toString).toArray(String[]::new);
            Commands.less(terminal, System.in, System.out, System.err, Paths.get(""), args);
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
