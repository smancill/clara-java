/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import org.jlab.clara.base.ClaraLang;
import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.base.DpeName;
import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.std.orchestrators.OrchestratorConfigException;
import org.jlab.clara.std.orchestrators.OrchestratorConfigParser;
import org.jlab.clara.util.FileUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class RunCommand extends BaseCommand {

    RunCommand(Context context) {
        super(context, "run", "Start Clara data processing");
        addSubCommand(RunLocal::new);
    }

    private static class RunLocal extends AbstractCommand {

        private static final int LOWER_PORT = 7000;
        private static final int UPPER_PORT = 8000;
        private static final int STEP_PORTS = 20;

        private final Map<ClaraLang, DpeProcess> backgroundDpes;
        private final RunUtils runUtils;

        RunLocal(Context context) {
            super(context, "local", "Run Clara data processing on the local node.");
            this.backgroundDpes = new HashMap<>();
            this.runUtils = new RunUtils(config);
        }

        @Override
        public int execute(String[] args) {
            try {
                var feDpe = startLocalDpes();
                var exitStatus = runOrchestrator(feDpe);
                if (Thread.interrupted()) {
                    destroyDpes();
                    Thread.currentThread().interrupt();
                }
                return exitStatus;
            } catch (OrchestratorConfigException e) {
                writer.println("Error: " + e.getMessage());
                return EXIT_ERROR;
            } catch (Exception e) {
                e.printStackTrace();
                return EXIT_ERROR;
            }
        }

        private int runOrchestrator(DpeName feName) {
            var cmd = orchestratorCmd(feName);
            var logFile = runUtils.getLogFile(getHost(feName), "orch");
            return CommandUtils.runProcess(buildProcess(cmd, logFile));
        }

        private String[] orchestratorCmd(DpeName feName) {
            var orchestrator = FileUtils.claraPath("bin", "clara-orchestrator");
            var cmd = new SystemCommandBuilder(orchestrator);

            cmd.addOption("-F");
            cmd.addOption("-f", feName);

            cmd.addOption("-t", getThreads());
            if (config.hasValue(Config.REPORT_EVENTS)) {
                cmd.addOption("-r", config.getInt(Config.REPORT_EVENTS));
            }
            if (config.hasValue(Config.SKIP_EVENTS)) {
                cmd.addOption("-k", config.getInt(Config.SKIP_EVENTS));
            }
            if (config.hasValue(Config.MAX_EVENTS)) {
                cmd.addOption("-e", config.getInt(Config.MAX_EVENTS));
            }
            cmd.addOption("-i", config.getString(Config.INPUT_DIR));
            cmd.addOption("-o", config.getString(Config.OUTPUT_DIR));

            cmd.addArgument(config.getString(Config.SERVICES_FILE));
            cmd.addArgument(config.getString(Config.FILES_LIST));

            return cmd.toArray();
        }

        private DpeName startLocalDpes() throws IOException {
            var configFile = config.getString(Config.SERVICES_FILE);
            var configParser = new OrchestratorConfigParser(Path.of(configFile));
            var languages = configParser.parseLanguages();

            if (checkDpes(languages)) {
                return backgroundDpes.get(ClaraLang.JAVA).name;
            }
            destroyDpes();

            var feName = new DpeName(findHost(), findPort(), ClaraLang.JAVA);
            var javaDpe = FileUtils.claraPath("bin", "j_dpe").toString();
            addBackgroundDpeProcess(feName, javaDpe,
                    "--host", getHost(feName),
                    "--port", getPort(feName),
                    "--session", runUtils.getSession());

            if (languages.contains(ClaraLang.CPP)) {
                var cppPort = feName.address().port() + 10;
                var cppName = new DpeName(feName.address().host(), cppPort, ClaraLang.CPP);
                var cppDpe = FileUtils.claraPath("bin", "c_dpe").toString();
                addBackgroundDpeProcess(cppName, cppDpe,
                        "--host", getHost(cppName),
                        "--port", getPort(cppName),
                        "--fe-host", getHost(feName),
                        "--fe-port", getPort(feName));
            }

            if (languages.contains(ClaraLang.PYTHON)) {
                var pyPort = feName.address().port() + 5;
                var pyName = new DpeName(feName.address().host(), pyPort, ClaraLang.PYTHON);
                var pyDpe = FileUtils.claraPath("bin", "p_dpe").toString();
                addBackgroundDpeProcess(pyName, pyDpe,
                        "--host", getHost(pyName),
                        "--port", getPort(pyName),
                        "--fe-host", getHost(feName),
                        "--fe-port", getPort(feName));
            }

            return feName;
        }

        private String findHost() {
            if (config.hasValue(Config.FRONTEND_HOST)) {
                return config.getString(Config.FRONTEND_HOST);
            }
            return ClaraUtil.localhost();
        }

        private int findPort() {
            if (config.hasValue(Config.FRONTEND_PORT)) {
                return config.getInt(Config.FRONTEND_PORT);
            }

            var ports = IntStream.iterate(LOWER_PORT, n -> n + STEP_PORTS)
                                 .limit((UPPER_PORT - LOWER_PORT) / STEP_PORTS)
                                 .boxed()
                                 .collect(Collectors.toList());
            Collections.shuffle(ports);

            for (var port : ports) {
                var ctrlPort = port + 2;
                try (var socket = new ServerSocket(ctrlPort)) {
                    socket.setReuseAddress(true);
                    return port;
                } catch (IOException e) {
                    // ignore
                }
            }
            throw new IllegalStateException("Cannot find an available port");
        }

        private boolean checkDpes(Set<ClaraLang> languages) {
            return languages.equals(backgroundDpes.keySet())
                && languages.stream().allMatch(this::isDpeAlive);
        }

        private boolean isDpeAlive(ClaraLang lang) {
            var dpeProcess = backgroundDpes.get(lang);
            return dpeProcess != null && dpeProcess.process.isAlive();
        }

        private void addBackgroundDpeProcess(DpeName name, String... command)
                throws IOException {
            if (!backgroundDpes.containsKey(name.language())) {
                var logFile = runUtils.getLogFile(name);
                var processBuilder = buildProcess(command, logFile);
                if (name.language() == ClaraLang.JAVA) {
                    var javaOptions = getJVMOptions();
                    if (javaOptions != null) {
                        processBuilder.environment().put("JAVA_OPTS", javaOptions);
                    }
                }
                runUtils.getMonitorFrontEnd().ifPresent(monFE ->
                    processBuilder.environment().put(ClaraConstants.ENV_MONITOR_FE, monFE)
                );
                var dpeProcess = new DpeProcess(name, processBuilder);
                backgroundDpes.put(name.language(), dpeProcess);
            }
        }

        private void destroyDpes() {
            // kill the DPEs in reverse order (the front-end last)
            for (var lang : List.of(ClaraLang.PYTHON, ClaraLang.CPP, ClaraLang.JAVA)) {
                var dpeProcess = backgroundDpes.remove(lang);
                if (dpeProcess == null) {
                    continue;
                }
                CommandUtils.destroyProcess(dpeProcess.process);
            }
        }

        private ProcessBuilder buildProcess(String[] command, Path logFile) {
            var wrapper = CommandUtils.uninterruptibleCommand(command, logFile);
            var builder = new ProcessBuilder(wrapper);
            builder.environment().putAll(config.getenv());
            builder.inheritIO();
            return builder;
        }

        private Integer getThreads() {
            if (config.hasValue(Config.MAX_THREADS)) {
                return config.getInt(Config.MAX_THREADS);
            }
            return Runtime.getRuntime().availableProcessors();
        }

        private String getJVMOptions() {
            if (config.hasValue(Config.JAVA_OPTIONS)) {
                return config.getString(Config.JAVA_OPTIONS);
            }
            if (config.hasValue(Config.JAVA_MEMORY)) {
                var memSize = config.getInt(Config.JAVA_MEMORY);
                return String.format("-Xms%dg -Xmx%dg -XX:+UseNUMA -XX:+UseBiasedLocking",
                                     memSize, memSize);
            }
            return null;
        }

        @Override
        public void close() {
            destroyDpes();
        }
    }

    private static class DpeProcess {

        private final DpeName name;
        private final Process process;

        DpeProcess(DpeName name, ProcessBuilder builder) throws IOException {
            this.name = name;
            this.process = builder.start();
            ClaraUtil.sleep(2000);
        }
    }

    private static String getHost(DpeName name) {
        return name.address().host();
    }

    private static String getPort(DpeName name) {
        return Integer.toString(name.address().port());
    }
}
