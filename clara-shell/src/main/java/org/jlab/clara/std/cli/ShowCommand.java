/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import org.jlab.clara.base.ClaraLang;
import org.jlab.clara.util.VersionUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class ShowCommand extends BaseCommand {

    ShowCommand(Context context) {
        super(context, "show", "Show values");
        setArguments();
    }

    private void setArguments() {
        addSubCommand("config", args -> showConfig(), "Show configuration variables.");
        addSubCommand("services", args -> showServices(), "Show services YAML.");
        addSubCommand("files", args -> showFiles(), "Show input files list.");
        addSubCommand("inputDir", args -> showInputDir(), "List input files directory.");
        addSubCommand("outputDir", args -> showOutputDir(), "List output files directory.");
        addSubCommand("logDir", args -> showLogDir(), "List logs directory.");
        addSubCommand("logDpe", args -> showDpeLog(), "Show front-end DPE log.");
        addSubCommand("logOrchestrator", args -> showOrchestratorLog(), "Show orchestrator log.");
        if (FarmCommands.hasPlugin()) {
            addSubCommand(FarmCommands.ShowFarmStatus::new);
            addSubCommand(FarmCommands.ShowFarmSub::new);
        }
        addSubCommand("version", args -> showVersion(), "Show Clara version.");
    }

    private int showConfig() {
        writer.println();
        config.getVariables().forEach(this::printFormat);
        return EXIT_SUCCESS;
    }

    private void printFormat(ConfigVariable variable) {
        System.out.printf("%-20s %s%n", variable.getName() + ":", getValue(variable));
    }

    private String getValue(ConfigVariable variable) {
        if (!variable.hasValue()) {
            return "NO VALUE";
        }
        Object value = variable.getValue();
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "\"" + value + "\"";
    }

    private int showServices() {
        return printFile(Config.SERVICES_FILE);
    }

    private int showFiles() {
        return printFile(Config.FILES_LIST);
    }

    private int showInputDir() {
        return listFiles(Config.INPUT_DIR, "lh");
    }

    private int showOutputDir() {
        return listFiles(Config.OUTPUT_DIR, "lh");
    }

    private int showLogDir() {
        return listFiles(Config.LOG_DIR, "lhtr");
    }

    private int showDpeLog() {
        return printLog("fe_dpe", "DPE");
    }

    private int showOrchestratorLog() {
        return printLog("orch", "orchestrator");
    }

    private int showVersion() {
        writer.println(VersionUtils.getClaraVersionFull());
        return EXIT_SUCCESS;
    }

    private int printFile(String variable) {
        if (!config.hasValue(variable)) {
            writer.printf("Error: variable %s is not set%n", variable);
            return EXIT_ERROR;
        }
        return RunUtils.printFile(terminal, config.getPath(variable));
    }

    private int listFiles(String variable, String options) {
        if (!config.hasValue(variable)) {
            writer.printf("Error: variable %s is not set%n", variable);
            return EXIT_ERROR;
        }
        return RunUtils.listFiles(config.getPath(variable), options);
    }

    private int printLog(String component, String description) {
        var runUtils = new RunUtils(config);
        List<Path> logs;
        try {
            logs = runUtils.getLogFiles(component);
        } catch (IOException e) {
            writer.printf("Error: could not open log directory%n");
            return EXIT_ERROR;
        }
        if (logs.isEmpty()) {
            writer.printf("Error: no logs for %s%n", runUtils.getSession());
            return EXIT_ERROR;
        }
        Path log = logs.get(0);
        if (component.equals("fe_dpe")) {
            return RunUtils.paginateFile(terminal, description, getDpeLogs(log));
        }
        return RunUtils.paginateFile(terminal, description, log);
    }

    private Path[] getDpeLogs(Path fe) {
        var logs = new ArrayList<Path>();
        logs.add(fe);
        var runUtils = new RunUtils(config);
        for (var lang : List.of(ClaraLang.CPP, ClaraLang.PYTHON)) {
            var path = runUtils.getLogFile(fe, lang);
            if (Files.exists(path)) {
                logs.add(path);
            }
        }
        return logs.toArray(new Path[0]);
    }
}
