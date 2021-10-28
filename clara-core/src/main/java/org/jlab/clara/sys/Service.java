/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys;

import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.engine.Engine;
import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.core.Callback;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Subscription;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.MetaDataProto.MetaData;
import org.jlab.clara.sys.RequestParser.RequestException;
import org.jlab.clara.sys.report.ServiceReport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A Clara service listening and executing requests.
 * <p>
 * An internal object pool contains N number of {@link ServiceEngine} objects,
 * where N is user specified value (usually equals to the number of cores).
 * A thread pool contains threads to run each object within.
 * Number of threads in the pool is equal to the size of the object pool.
 * Thread pool is fixed size, however object pool is capable of expanding.
 */
class Service extends AbstractActor {

    private final String name;
    private final Engine userEngine;

    private final ExecutorService executionPool;
    private final ServiceEngine[] enginePool;
    private final ServiceSysConfig sysConfig;
    private final ServiceReport sysReport;

    private Subscription subscription;

    /**
     * Constructor of a service.
     * <p>
     * Create thread pool to run requests to this service.
     * Create object pool to hold the engines this service.
     * Object pool size is set to be 2 in case it was requested
     * to be 0 or negative number.
     *
     * @throws ClaraException
     */
    Service(ClaraComponent comp,
            ClaraComponent frontEnd,
            ConnectionPools connectionPools,
            String session) throws ClaraException {
        super(comp, frontEnd);

        name = comp.getCanonicalName();
        sysConfig = new ServiceSysConfig(name, comp.getInitialState());

        // Dynamic loading of the Clara engine class
        // Note: using system class loader
        EngineLoader cl = new EngineLoader(ClassLoader.getSystemClassLoader());
        userEngine = cl.load(comp.getEngineClass());

        sysReport = new ServiceReport(comp, userEngine, session);

        // Creating thread pool
        executionPool = ActorUtils.newThreadPool(comp.getSubscriptionPoolSize(), name);

        // Creating service object pool
        enginePool = new ServiceEngine[comp.getSubscriptionPoolSize()];

        // Fill the object pool
        ServiceActor engineActor = new ServiceActor(comp, frontEnd, connectionPools);
        for (int i = 0; i < comp.getSubscriptionPoolSize(); i++) {
            enginePool[i] = new ServiceEngine(userEngine, engineActor, sysConfig, sysReport);
        }

        // Register with the shared memory
        SharedMemory.addReceiver(name);
    }


    @Override
    void initialize() throws ClaraException {
        base.cacheLocalConnection();

        // start the engines
        try {
            Arrays.stream(enginePool).parallel().forEach(s -> {
                try {
                    s.start();
                } catch (ClaraException e) {
                    throwWrapped(e);
                }
            });
        } catch (WrappedException e) {
            throw e.getCause();
        }

        // subscribe and register
        Topic topic = base.getMe().getTopic();
        Callback callback = new ServiceCallBack();
        String description = base.getDescription();
        subscription = startRegisteredSubscription(topic, callback, description);
    }


    @Override
    void end() {
        stopSubscription();
        destroyEngines();
    }


    @Override
    void startMsg() {
        Logging.info("started service = %s  pool_size = %d", name, base.getPoolSize());
    }


    @Override
    void stopMsg() {
        Logging.info("removed service = %s", name);
    }


    private void configure(final Message msg) throws Exception {
        while (true) {
            for (final ServiceEngine engine : enginePool) {
                if (engine.tryAcquire()) {
                    executionPool.submit(() -> {
                        try {
                            engine.configure(msg);
                        } catch (Exception e) {
                            printUnhandledException(e);
                        } finally {
                            engine.release();
                        }
                    });
                    return;
                }
            }
        }
    }


    private void execute(final Message msg) {
        while (true) {
            for (final ServiceEngine engine : enginePool) {
                if (engine.tryAcquire()) {
                    executionPool.submit(() -> {
                        try {
                            engine.execute(msg);
                        } catch (Exception e) {
                            printUnhandledException(e);
                        } finally {
                            engine.release();
                        }
                    });
                    return;
                }
            }
        }
    }


    private void setup(Message msg) throws RequestException {
        RequestParser setup = RequestParser.build(msg);
        String report = setup.nextString();
        int value = setup.nextInteger();
        boolean publishReport = value > 0; // 0 is used to cancel reports
        switch (report) {
            case ClaraConstants.SERVICE_REPORT_DONE:
                sysConfig.setDoneRequest(publishReport);
                sysConfig.setDoneReportThreshold(value);
                sysConfig.resetDoneRequestCount();
                break;
            case ClaraConstants.SERVICE_REPORT_DATA:
                sysConfig.setDataRequest(publishReport);
                sysConfig.setDataReportThreshold(value);
                sysConfig.resetDataRequestCount();
                break;
            case ClaraConstants.SERVICE_REPORT_RING:
                sysConfig.setRingRequest(publishReport);
                break;
            default:
                throw new RequestException("Invalid report request: " + report);
        }
        if (msg.hasReplyTopic()) {
            sendResponse(msg, MetaData.Status.INFO, setup.request());
        }
    }


    private void printUnhandledException(Exception e) {
        StringWriter errors = new StringWriter();
        errors.write(name + ": Clara error: ");
        e.printStackTrace(new PrintWriter(errors));
        System.err.println(errors.toString());
    }


    void setFrontEnd(ClaraComponent frontEnd) {
        base.setFrontEnd(frontEnd);
    }

    ServiceReport getReport() {
        return sysReport;
    }


    private void stopSubscription() {
        if (subscription != null) {
            base.stopListening(subscription);
            base.stopCallbacks();
            try {
                if (shouldDeregister()) {
                    base.removeRegistration(base.getMe().getTopic());
                }
            } catch (ClaraException e) {
                Logging.error("service = %s: %s", name, e.getMessage());
            }
        }
    }


    private void destroyEngines() {
        destroyPool();
        Arrays.stream(enginePool).parallel().forEach(ServiceEngine::stop);
        userEngine.destroy();
    }


    private void destroyPool() {
        executionPool.shutdown();
        try {
            if (!executionPool.awaitTermination(10, TimeUnit.SECONDS)) {
                executionPool.shutdownNow();
                if (!executionPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    Logging.error("service = %s: execution pool did not terminate", name);
                }
            }
        } catch (InterruptedException ie) {
            executionPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    private class ServiceCallBack implements Callback {

        @Override
        public void callback(Message msg) {
            try {
                MetaData.Builder metadata = msg.getMetaData();
                if (!metadata.hasAction()) {
                    setup(msg);
                } else if (metadata.getAction().equals(MetaData.ControlAction.CONFIGURE)) {
                    configure(msg);
                } else {
                    execute(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (msg.hasReplyTopic()) {
                    sendResponse(msg, MetaData.Status.ERROR, e.getMessage());
                }
            }
        }
    }
}
