/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys;

import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.base.core.DataUtil;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.engine.Engine;
import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.EngineDataType;
import org.jlab.clara.engine.EngineStatus;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.MetaDataProto.MetaData;
import org.jlab.clara.sys.ccc.CompositionCompiler;
import org.jlab.clara.sys.ccc.ServiceState;
import org.jlab.clara.sys.report.ServiceReport;

import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * A Service engine.
 * Every engine process a request in its own thread.
 *
 * @author gurjyan
 * @version 4.x
 */
class ServiceEngine {

    private final Engine engine;
    private final ServiceActor base;

    private final ServiceSysConfig sysConfig;
    private final ServiceReport sysReport;

    private final Semaphore semaphore = new Semaphore(1);

    private final CompositionCompiler compiler;

    private final ClaraComponent monitorFe;

    // Already recorded (previous) composition
    private String prevComposition = ClaraConstants.UNDEFINED;

    // The last execution time
    private long executionTime;


    ServiceEngine(Engine userEngine,
                  ServiceActor base,
                  ServiceSysConfig config,
                  ServiceReport report) {
        this.base = base;
        this.engine = userEngine;
        this.sysConfig = config;
        this.sysReport = report;
        this.compiler = new CompositionCompiler(base.getName());

        this.monitorFe = FrontEnd.getMonitorFrontEnd()
                                 .map(dpe -> ClaraComponent.dpe(dpe.canonicalName()))
                                 .orElse(null);
    }

    void start() throws ClaraException {
        // nothing
    }

    void stop() {
        // nothing
    }

    public void configure(Message message) throws ClaraException {

        EngineData inputData;
        EngineData outData = null;
        try {
            inputData = getEngineData(message);
            outData = configureEngine(inputData);
        } catch (Exception e) {
            Logging.error("UNHANDLED EXCEPTION ON SERVICE CONFIGURATION: %s", base.getName());
            e.printStackTrace();
            outData = DataUtil.buildErrorData("unhandled exception", 4, e);
        } catch (Throwable e) {
            Logging.error("UNHANDLED CRITICAL ERROR ON SERVICE CONFIGURATION: %s", base.getName());
            e.printStackTrace();
            outData = DataUtil.buildErrorData("unhandled critical error", 4, e);
        } finally {
            updateMetadata(message.getMetaData(), DataUtil.getMetadata(outData));
            resetClock();
        }

        String replyTo = getReplyTo(message);
        if (replyTo != null) {
            sendResponse(outData, replyTo);
        } else {
            reportProblem(outData);
        }
    }


    private EngineData configureEngine(EngineData inputData) {
        var startTime = startClock();

        var outData = engine.configure(inputData);

        stopClock(startTime);

        if (outData == null) {
            outData = new EngineData();
        }
        if (outData.getData() == null) {
            outData.setData(EngineDataType.STRING.mimeType(), "done");
        }

        return outData;
    }


    public void execute(Message message) throws ClaraException {
        sysConfig.addRequest();
        sysReport.incrementRequestCount();

        EngineData inData = null;
        EngineData outData = null;

        try {
            inData = getEngineData(message);
            parseComposition(inData);
            outData = executeEngine(inData);
            sysReport.addExecutionTime(executionTime);
        } catch (Exception e) {
            Logging.error("UNHANDLED EXCEPTION ON SERVICE EXECUTION: %s", base.getName());
            e.printStackTrace();
            outData = DataUtil.buildErrorData("unhandled exception", 4, e);
        } catch (Throwable e) {
            Logging.error("UNHANDLED CRITICAL ERROR ON SERVICE EXECUTION: %s", base.getName());
            e.printStackTrace();
            outData = DataUtil.buildErrorData("unhandled critical error", 4, e);
        } finally {
            updateMetadata(message.getMetaData(), DataUtil.getMetadata(outData));
            resetClock();
        }

        String replyTo = getReplyTo(message);
        if (replyTo != null) {
            sendResponse(outData, replyTo);
            return;
        }

        reportProblem(outData);
        if (outData.getStatus() == EngineStatus.ERROR) {
            sysReport.incrementFailureCount();
            return;
        }

        reportResult(outData);

        if (sysConfig.isRingRequest()) {
            String executionState = outData.getExecutionState();
            if (!executionState.isEmpty()) {
                sendResult(inData, getLinks(inData, outData));
                sendMonitorData(executionState, outData);
            } else {
                sendResult(outData, getLinks(inData, outData));
            }
        } else {
            sendResult(outData, getLinks(inData, outData));
        }
    }

    private void parseComposition(EngineData inData) throws ClaraException {
        var currentComposition = inData.getComposition();
        if (!currentComposition.equals(prevComposition)) {
            compiler.compile(currentComposition);
            prevComposition = currentComposition;
        }
    }

    private Set<String> getLinks(EngineData inData, EngineData outData) {
        var ownerState = new ServiceState(outData.getEngineName(),
                                          outData.getExecutionState());
        var inputState = new ServiceState(inData.getEngineName(),
                                          inData.getExecutionState());

        return compiler.getLinks(ownerState, inputState);
    }

    private EngineData executeEngine(EngineData inData)
            throws ClaraException {
        var startTime = startClock();

        var outData = engine.execute(inData);

        stopClock(startTime);

        if (outData == null) {
            throw new ClaraException("null engine result");
        }
        if (outData.getData() == null) {
            if (outData.getStatus() == EngineStatus.ERROR) {
                outData.setData(EngineDataType.STRING.mimeType(),
                                ClaraConstants.UNDEFINED);
            } else {
                throw new ClaraException("empty engine result");
            }
        }

        return outData;
    }

    private void updateMetadata(MetaData.Builder inMeta, MetaData.Builder outMeta) {
        outMeta.setAuthor(base.getName());
        outMeta.setVersion(engine.getVersion());

        if (!outMeta.hasCommunicationId()) {
            outMeta.setCommunicationId(inMeta.getCommunicationId());
        }
        outMeta.setComposition(inMeta.getComposition());
        outMeta.setExecutionTime(executionTime);
        outMeta.setAction(inMeta.getAction());

        if (outMeta.hasSenderState()) {
            sysConfig.updateState(outMeta.getSenderState());
        }
    }

    private void reportResult(EngineData outData) throws ClaraException {
        if (sysConfig.isDataRequest()) {
            reportData(outData);
            sysConfig.resetDataRequestCount();
        }
        if (sysConfig.isDoneRequest()) {
            reportDone(outData);
            sysConfig.resetDoneRequestCount();
        }
    }

    private void sendResponse(EngineData outData, String replyTo) throws ClaraException {
        base.send(putEngineData(outData, replyTo));
    }

    private void sendResult(EngineData outData, Set<String> outLinks) throws ClaraException {
        for (var service : outLinks) {
            var dpe = ClaraComponent.dpe(service);
            var msg = putEngineData(outData, service);
            base.send(dpe.getProxyAddress(), msg);
        }
    }

    private void reportDone(EngineData data) throws ClaraException {
        var mt = data.getMimeType();
        var obj = data.getData();
        data.setData(EngineDataType.STRING.mimeType(), ClaraConstants.DONE);

        sendReport(ClaraConstants.DONE, data);

        data.setData(mt, obj);
    }

    private void reportData(EngineData data) throws ClaraException {
        sendReport(ClaraConstants.DATA, data);
    }

    private void reportProblem(EngineData data) throws ClaraException {
        var status = data.getStatus();
        if (status.equals(EngineStatus.ERROR)) {
            sendReport(ClaraConstants.ERROR, data);
        } else if (status.equals(EngineStatus.WARNING)) {
            sendReport(ClaraConstants.WARNING, data);
        }
    }


    private void sendReport(String topicPrefix, EngineData data) throws ClaraException {
        var topic = Topic.wrap(topicPrefix + Topic.SEPARATOR + base.getName());
        var msg = DataUtil.serialize(topic, data, engine.getOutputDataTypes());
        base.send(base.getFrontEnd(), msg);
    }

    private void sendMonitorData(String state, EngineData data) throws ClaraException {
        if (monitorFe != null) {
            var topic = Topic.wrap(ClaraConstants.MONITOR_REPORT
                    + Topic.SEPARATOR + state
                    + Topic.SEPARATOR + sysReport.getSession()
                    + Topic.SEPARATOR + base.getEngine());
            var msg = DataUtil.serialize(topic, data, engine.getOutputDataTypes());
            base.sendUncheck(monitorFe.getProxyAddress(), msg);
        }
    }


    private EngineData getEngineData(Message message) throws ClaraException {
        var metadata = message.getMetaData();
        var mimeType = metadata.getDataType();
        if (mimeType.equals(ClaraConstants.SHARED_MEMORY_KEY)) {
            sysReport.incrementShrmReads();
            var sender = metadata.getSender();
            var id = metadata.getCommunicationId();
            return SharedMemory.getEngineData(base.getName(), sender, id);
        } else {
            sysReport.addBytesReceived(message.getDataSize());
            return DataUtil.deserialize(message, engine.getInputDataTypes());
        }
    }

    private Message putEngineData(EngineData data, String receiver)
            throws ClaraException {
        var topic = Topic.wrap(receiver);
        if (SharedMemory.containsReceiver(receiver)) {
            var id = data.getCommunicationId();
            SharedMemory.putEngineData(receiver, base.getName(), id, data);
            sysReport.incrementShrmWrites();

            var metadata = MetaData.newBuilder();
            metadata.setAuthor(base.getName());
            metadata.setComposition(data.getComposition());
            metadata.setCommunicationId(id);
            metadata.setAction(MetaData.ControlAction.EXECUTE);
            metadata.setDataType(ClaraConstants.SHARED_MEMORY_KEY);

            return new Message(topic, metadata, ClaraConstants.SHARED_MEMORY_KEY.getBytes());
        } else {
            var msg = DataUtil.serialize(topic, data, engine.getOutputDataTypes());
            sysReport.addBytesSent(msg.getDataSize());
            return msg;
        }
    }


    private String getReplyTo(Message message) {
        var meta = message.getMetaData();
        if (meta.hasReplyTo()) {
            return meta.getReplyTo();
        }
        return null;
    }


    private void resetClock() {
        executionTime = 0;
    }

    private long startClock() {
        return System.nanoTime();
    }

    private void stopClock(long watch) {
        executionTime = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - watch);
    }


    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    public void release() {
        semaphore.release();
    }
}
