/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators;

import org.jlab.clara.base.ClaraLang;
import org.jlab.clara.base.ServiceName;
import org.jlab.clara.base.ServiceRuntimeData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Benchmark {

    private record ServiceKey(String engine, String container, ClaraLang lang) { }

    private static class Runtime {
        long initialTime = 0;
        long totalTime = 0;
    }

    private final Map<ServiceKey, Runtime> runtimeStats = new HashMap<>();

    Benchmark(ApplicationInfo application) {
        var services = allServices(application);
        services.forEach(s -> runtimeStats.put(key(s), new Runtime()));
    }

    private static List<ServiceInfo> allServices(ApplicationInfo application) {
        var services = new ArrayList<ServiceInfo>();
        services.add(application.getReaderService());
        services.addAll(application.getDataProcessingServices());
        services.add(application.getWriterService());
        return services;
    }

    void initialize(Set<ServiceRuntimeData> data) {
        data.forEach(s -> {
            Runtime r = runtimeStats.get(key(s));
            if (r != null) {
                r.initialTime = s.executionTime();
            }
        });
    }

    void update(Set<ServiceRuntimeData> data) {
        data.forEach(s -> {
            Runtime r = runtimeStats.get(key(s));
            if (r != null) {
                r.totalTime = s.executionTime();
            }
        });
    }

    long time(ServiceInfo service) {
        Runtime r = runtimeStats.get(key(service));
        if (r != null) {
            return r.totalTime - r.initialTime;
        }
        throw new OrchestratorException("Invalid runtime report: missing " + service.name());
    }

    private static ServiceKey key(ServiceInfo service) {
        return new ServiceKey(service.name(), service.cont(), service.lang());
    }

    private static ServiceKey key(ServiceRuntimeData serviceData) {
        return key(serviceData.name());
    }

    private static ServiceKey key(ServiceName service) {
        return new ServiceKey(service.name(), service.container().name(), service.language());
    }
}
