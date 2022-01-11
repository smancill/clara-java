/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys;

import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.base.core.MessageUtil;
import org.jlab.clara.base.error.ClaraException;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.sys.report.ContainerReport;
import org.jlab.clara.util.EnvUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service container.
 *
 * @author gurjyan
 * @version 4.x
 */
class Container extends AbstractActor {

    private final ConcurrentHashMap<String, Service> myServices = new ConcurrentHashMap<>();
    private final ContainerReport myReport;

    private boolean isRegistered = false;

    Container(ClaraComponent comp, ClaraComponent frontEnd) {
        super(comp, frontEnd);

        myReport = new ContainerReport(base, EnvUtils.userName());
    }

    @Override
    void initialize() throws ClaraException {
        register();
    }

    @Override
    void end() {
        removeAllServices();
        removeRegistration();
    }

    @Override
    void startMsg() {
        Logging.info("started container = %s", base.getName());
    }

    @Override
    void stopMsg() {
        Logging.info("removed container = %s", base.getName());
    }

    public void addService(ClaraComponent comp,
                           ClaraComponent frontEnd,
                           ConnectionPools connectionPools,
                           String session) throws ClaraException {
        String serviceName = comp.getCanonicalName();
        Service service = myServices.get(serviceName);
        if (service == null) {
            service = new Service(comp, frontEnd, connectionPools, session);
            Service prev = myServices.putIfAbsent(serviceName, service);
            if (prev == null) {
                try {
                    service.start();
                    myReport.addService(service.getReport());
                } catch (ClaraException e) {
                    service.stop();
                    myServices.remove(serviceName, service);
                    throw e;
                }
            } else {
                service.stop();    // destroy the extra engine object
            }
        } else {
            Logging.error("service = %s already exists. No new service is deployed", serviceName);
        }
    }

    public boolean removeService(String serviceName) {
        Service service = myServices.remove(serviceName);
        if (service != null) {
            service.stop();
            myReport.removeService(service.getReport());
            return true;
        }
        return false;
    }

    private void removeAllServices() {
        myServices.values().parallelStream().forEach(Service::stop);
        myServices.clear();
    }

    private void register() throws ClaraException {
        Topic topic = Topic.build(ClaraConstants.CONTAINER, base.getName());
        base.register(topic, base.getDescription());
        isRegistered = true;
    }

    private void removeRegistration() {
        if (isRegistered && shouldDeregister()) {
            try {
                reportDown();
                base.removeRegistration(base.getMe().getTopic());
            } catch (ClaraException e) {
                Logging.error("container = %s: %s", base.getName(), e.getMessage());
            } finally {
                isRegistered = false;
            }
        }
    }

    private void reportDown() {
        try {
            // broadcast to the local proxy
            String data = MessageUtil.buildData(ClaraConstants.CONTAINER_DOWN, base.getName());
            base.send(base.getFrontEnd(), data);
        } catch (ClaraMsgException e) {
            Logging.error("container = %s: could not send down report: %s",
                          base.getName(), e.getMessage());
        }
    }

    public void setFrontEnd(ClaraComponent frontEnd) {
        base.setFrontEnd(frontEnd);
    }

    public ConcurrentHashMap<String, Service> geServices() {
        return myServices;
    }

    public ContainerReport getReport() {
        return myReport;
    }
}
