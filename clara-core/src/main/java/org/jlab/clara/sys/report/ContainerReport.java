/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys.report;

import org.jlab.clara.base.core.ClaraBase;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gurjyan
 * @version 4.x
 */
public class ContainerReport extends BaseReport {

    private final Map<String, ServiceReport> services = new ConcurrentHashMap<>();

    public ContainerReport(ClaraBase base, String author) {
        super(base.getName(), author, base.getDescription());
    }

    public int getServiceCount() {
        return services.size();
    }

    public Collection<ServiceReport> getServices() {
        return services.values();
    }

    public ServiceReport addService(ServiceReport sr) {
        return services.putIfAbsent(sr.getName(), sr);
    }

    public ServiceReport removeService(ServiceReport sr) {
        return services.remove(sr.getName());
    }

    public void removeAllServices() {
        services.clear();
    }
}
