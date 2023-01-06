/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.base.ClaraLang;
import org.jlab.clara.base.DpeName;
import org.jlab.clara.base.EngineCallback;
import org.jlab.clara.base.ServiceName;
import org.jlab.clara.base.ServiceRuntimeData;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.EngineDataType;
import org.jlab.clara.engine.EngineStatus;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;


class WorkerNode {

    private final CoreOrchestrator orchestrator;
    private final WorkerApplication application;

    private final ServiceName stageName;
    private final ServiceName readerName;
    private final ServiceName writerName;

    private volatile JSONObject userConfig = new JSONObject();

    private volatile String currentInputFileName;
    private volatile String currentInputFile;
    private volatile String currentOutputFile;

    final AtomicInteger currentFileCounter = new AtomicInteger();
    final AtomicInteger totalFilesCounter = new AtomicInteger();

    final AtomicInteger skipEvents = new AtomicInteger();
    final AtomicInteger maxEvents = new AtomicInteger();

    final AtomicInteger totalEvents = new AtomicInteger();
    final AtomicInteger eventNumber = new AtomicInteger();
    final AtomicInteger eofCounter = new AtomicInteger();

    final AtomicLong startTime = new AtomicLong();
    final AtomicLong lastReportTime = new AtomicLong();


    static class Builder {

        private final ApplicationInfo app;
        private final Map<ClaraLang, DpeInfo> dpes;

        private boolean ready = false;

        Builder(ApplicationInfo application) {
            app = application;
            dpes = new HashMap<>();
        }

        public void addDpe(DpeInfo dpe) {
            var lang = dpe.name().language();
            if (!app.getLanguages().contains(lang)) {
                Logging.info("Ignoring DPE %s (language not needed)", dpe.name());
                return;
            }
            var prev = dpes.put(lang, dpe);
            if (prev != null && !prev.equals(dpe)) {
                Logging.info("Replacing existing DPE %s with %s", prev.name(), dpe.name());
            }
            ready = checkReady();
        }

        public boolean isReady() {
            return ready;
        }

        public WorkerNode build(CoreOrchestrator orchestrator) {
            return new WorkerNode(orchestrator, new WorkerApplication(app, dpes));
        }

        private boolean checkReady() {
            return app.getLanguages().stream().allMatch(dpes::containsKey);
        }
    }


    WorkerNode(CoreOrchestrator orchestrator, WorkerApplication application) {
        if (orchestrator == null) {
            throw new IllegalArgumentException("Null orchestrator parameter");
        }
        if (application == null) {
            throw new IllegalArgumentException("Null application parameter");
        }

        this.application = application;
        this.orchestrator = orchestrator;

        this.stageName = application.stageService();
        this.readerName = application.readerService();
        this.writerName = application.writerService();
    }


    void deployServices() {
        application.getInputOutputServicesDeployInfo().forEach(orchestrator::deployService);
        application.getProcessingServicesDeployInfo().forEach(orchestrator::deployService);
        application.getMonitoringServicesDeployInfo().forEach(orchestrator::deployService);

        application.allServices().forEach(orchestrator::checkServices);
    }


    boolean checkServices() {
        return application.allServices().entrySet().stream()
                .allMatch(e -> orchestrator.findServices(e.getKey(), e.getValue()));
    }


    void subscribeErrors(Function<WorkerNode, EngineCallback> callbackFactory) {
        var callback = callbackFactory.apply(this);
        application.allContainers().values().stream()
                   .flatMap(Collection::stream)
                   .forEach(cont -> orchestrator.subscribeErrors(cont, callback));
    }


    void subscribeDone(Function<WorkerNode, EngineCallback> callbackFactory) {
        orchestrator.subscribeDone(writerName, callbackFactory.apply(this));
    }


    void setConfiguration(JSONObject configData) {
        this.userConfig = configData;
    }


    private ApplicationConfig createApplicationConfig(boolean fillDataModel) {
        var model = new HashMap<String, Object>();
        if (fillDataModel && currentInputFile != null) {
            model.put("input_file", currentInputFile);
            model.put("output_file", currentOutputFile);
        }

        return new ApplicationConfig(userConfig, model);
    }


    void setPaths(Path inputPath, Path outputPath, Path stagePath) {
        try {
            var data = new JSONObject();
            data.put("input_path", inputPath);
            data.put("output_path", outputPath);
            data.put("stage_path", stagePath);
            orchestrator.syncConfig(stageName, data, 2, TimeUnit.MINUTES);
        } catch (ClaraException | TimeoutException e) {
            throw new OrchestratorException("Could not configure directories", e);
        }
    }


    void setFiles(FileInfo currentFile) {
        try {
            var request = new JSONObject();
            request.put("type", "exec");
            request.put("action", "stage_input");
            request.put("file", currentFile.inputName);

            Logging.info("Staging file %s on %s", currentFile.inputName, name());
            var response = orchestrator.syncSend(stageName, request, 5, TimeUnit.MINUTES);

            if (!response.getStatus().equals(EngineStatus.ERROR)) {
                var content = (String) response.getData();
                var data = new JSONObject(content);
                currentInputFile = data.getString("input_file");
                currentOutputFile = data.getString("output_file");
                currentInputFileName = currentFile.inputName;
            } else {
                var error = "Could not stage input file: " + response.getDescription();
                throw new OrchestratorException(error);
            }
        } catch (ClaraException | TimeoutException e) {
            throw new OrchestratorException("Could not stage input file", e);
        }
    }


    void setFiles(OrchestratorPaths paths, FileInfo currentFile) {
        currentInputFile = paths.inputFilePath(currentFile).toString();
        currentOutputFile = paths.outputFilePath(currentFile).toString();
        currentInputFileName = currentFile.inputName;
    }


    void setFileCounter(int currentFile, int totalFiles) {
        currentFileCounter.set(currentFile);
        totalFilesCounter.set(totalFiles);
    }


    void clearFiles() {
        currentInputFile = null;
        currentOutputFile = null;
        currentInputFileName = null;
    }


    String currentFile() {
        return currentInputFileName;
    }


    boolean saveOutputFile() {
        try {
            var cleanRequest = new JSONObject();
            cleanRequest.put("type", "exec");
            cleanRequest.put("action", "remove_input");
            cleanRequest.put("file", currentInputFileName);
            EngineData cleanResponse = orchestrator.syncSend(stageName, cleanRequest, 5, TimeUnit.MINUTES);

            var saveRequest = new JSONObject();
            saveRequest.put("type", "exec");
            saveRequest.put("action", "save_output");
            saveRequest.put("file", currentInputFileName);
            EngineData saveResponse = orchestrator.syncSend(stageName, saveRequest, 5, TimeUnit.MINUTES);

            boolean status = true;
            if (cleanResponse.getStatus().equals(EngineStatus.ERROR)) {
                System.err.println(cleanResponse.getDescription());
                status = false;
            }
            if (saveResponse.getStatus().equals(EngineStatus.ERROR)) {
                status = false;
                System.err.println(saveResponse.getDescription());
            }

            return status;
        } catch (ClaraException | TimeoutException e) {
            throw new OrchestratorException("Could not save output", e);
        }
    }


    boolean removeStageDir() {
        try {
            var request = new JSONObject();
            request.put("type", "exec");
            request.put("action", "clear_stage");

            var response = orchestrator.syncSend(stageName, request, 5, TimeUnit.MINUTES);

            if (response.getStatus().equals(EngineStatus.ERROR)) {
                Logging.error("Failed to remove stage directory on %s: %s",
                        name(), response.getDescription());
                return false;
            }
            return true;
        } catch (ClaraException | TimeoutException e) {
            Logging.error("Failed to remove stage directory on %s: %s", name(), e.getMessage());
            return false;
        }
    }


    void setEventLimits(int skipEvents, int maxEvents) {
        this.skipEvents.set(skipEvents);
        this.maxEvents.set(maxEvents);
    }


    void openFiles() {
        startTime.set(0);
        lastReportTime.set(0);
        eofCounter.set(0);
        eventNumber.set(0);
        totalEvents.set(0);

        var configData = createApplicationConfig(false);

        var skipEvents = this.skipEvents.get();
        var maxEvents = this.maxEvents.get();

        // open input file
        try {
            Logging.info("Opening file %s on %s", currentInputFileName, name());
            var config = configData.reader();
            config.put("action", "open");
            config.put("file", currentInputFile);
            if (skipEvents > 0) {
                config.put("skip", skipEvents);
            }
            if (maxEvents > 0) {
                config.put("max", maxEvents);
            }
            orchestrator.syncConfig(readerName, config, 5, TimeUnit.MINUTES);
        } catch (OrchestratorConfigException e) {
            throw new OrchestratorException("Could not configure reader", e);
        } catch (ClaraException | TimeoutException e) {
            throw new OrchestratorException("Could not open input file", e);
        }

        // total number of events in the file
        var totalEvents = requestNumberOfEvents() - skipEvents;
        if (maxEvents > 0 && maxEvents < totalEvents) {
            totalEvents = maxEvents;
        }
        this.totalEvents.set(totalEvents);

        // endianness of the file
        var fileOrder = requestFileOrder();

        // open output file
        try {
            var config = configData.writer();
            config.put("action", "open");
            config.put("file", currentOutputFile);
            config.put("order", fileOrder);
            config.put("overwrite", true);
            orchestrator.syncConfig(writerName, config, 5, TimeUnit.MINUTES);
        } catch (OrchestratorConfigException e) {
            throw new OrchestratorException("Could not configure writer", e);
        } catch (ClaraException | TimeoutException e) {
            throw new OrchestratorException("Could not open output file", e);
        }
    }


    void setReportFrequency(int frequency) {
        // set "report done" frequency
        if (frequency <= 0) {
            return;
        }
        try {
            orchestrator.startDoneReporting(writerName, frequency);
        } catch (ClaraException e) {
            throw new OrchestratorException("Could not configure writer", e);
        }
    }


    void closeFiles() {
        try {
            var config = new JSONObject();
            config.put("action", "close");
            config.put("file", currentInputFile);
            orchestrator.syncConfig(readerName, config, 5, TimeUnit.MINUTES);
        } catch (ClaraException | TimeoutException e) {
            throw new OrchestratorException("Could not close input file", e);
        }

        try {
            var config = new JSONObject();
            config.put("action", "close");
            config.put("file", currentOutputFile);
            orchestrator.syncConfig(writerName, config, 5, TimeUnit.MINUTES);
        } catch (ClaraException | TimeoutException e) {
            throw new OrchestratorException("Could not close output file", e);
        }
    }


    private String requestFileOrder() {
        try {
            var response = orchestrator.syncSend(readerName, "order", 1, TimeUnit.MINUTES);
            return (String) response.getData();
        } catch (ClaraException | TimeoutException e) {
            throw new OrchestratorException("Could not get input file order", e);
        }
    }


    private int requestNumberOfEvents() {
        try {
            var response = orchestrator.syncSend(readerName, "count", 1, TimeUnit.MINUTES);
            return (Integer) response.getData();
        } catch (ClaraException | TimeoutException e) {
            throw new OrchestratorException("Could not get number of input events", e);
        }
    }


    void configureServices() {
        var configData = createApplicationConfig(true);

        for (var service : application.services()) {
            try {
                orchestrator.syncConfig(service, configData.get(service), 2, TimeUnit.MINUTES);
            } catch (OrchestratorConfigException e) {
                throw new OrchestratorException("Could not configure " + service, e);
            } catch (ClaraException | TimeoutException e) {
                throw new OrchestratorException("Could not configure " + service, e);
            }
        }

        for (var service : application.monitoringServices()) {
            try {
                orchestrator.syncEnableRing(service, 1, TimeUnit.MINUTES);
            } catch (ClaraException | TimeoutException e) {
                throw new OrchestratorException("Could not configure " + service, e);
            }
        }
    }


    void sendEvents(int maxCores) {
        var currentTime = System.currentTimeMillis();
        startTime.compareAndSet(0, currentTime);
        lastReportTime.compareAndSet(0, currentTime);

        var requestCores = numCores(maxCores);
        var requestId = 1;

        Logging.info("Using %d cores on %s to process %d events of %s [%d/%d]",
                      requestCores, name(), totalEvents.get(), currentInputFileName,
                      currentFileCounter.get(), totalFilesCounter.get());

        for (int i = 0; i < requestCores; i++) {
            requestEvent(requestId++, "next");
        }
    }


    void requestEvent(int requestId, String type) {
        try {
            var request = new EngineData();
            request.setData(EngineDataType.STRING.mimeType(), type);
            request.setCommunicationId(requestId);
            orchestrator.send(application.composition(), request);
        } catch (ClaraException e) {
            throw new OrchestratorException("Could not send an event request to = " + name(), e);
        }
    }


    private int numCores(int maxCores) {
        int appCores = application.maxCores();
        return Math.min(appCores, maxCores);
    }


    int maxCores() {
        return application.maxCores();
    }


    Set<ServiceRuntimeData> getRuntimeData() {
        return application.dpes().stream()
               .map(orchestrator::getReport)
               .flatMap(Set::stream)
               .collect(Collectors.toSet());
    }


    boolean isFrontEnd() {
        var frontEndHost = orchestrator.getFrontEnd().address().host();
        return frontEndHost.equals(name());
    }


    String name() {
        return application.hostName();
    }


    Set<DpeName> dpes() {
        return Collections.unmodifiableSet(application.dpes());
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + application.hashCode();
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof WorkerNode other)) {
            return false;
        }
        if (!application.equals(other.application)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return application.toString();
    }
}
